package services

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import com.cjwwdev.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate, MongoUpdatedResponse}
import com.cjwwdev.security.SecurityConfiguration
import com.cjwwdev.security.deobfuscation.DeObfuscators
import database.{UserStore, UserStoreUtils}
import javax.inject.{Inject, Named}
import models.{Address, AuthorisedClient, Gender, Name, UserInfo}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Updates.{set, unset}
import org.slf4j.{Logger, LoggerFactory}
import utils.StringUtils
import utils.BsonUtils._
import java.text.SimpleDateFormat

import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.jdk.CollectionConverters._

sealed trait LinkResponse
case object LinkSuccess extends LinkResponse
case object LinkFailed extends LinkResponse
case object LinkExists extends LinkResponse
case object LinkRemoved extends LinkResponse
case object NoUserFound extends LinkResponse

class DefaultUserService @Inject()(@Named("individualUserStore") val individualUserStore: UserStore,
                                   @Named("organisationUserStore") val organisationUserStore: UserStore) extends UserService {
  override val locale: String = ""
}

trait UserService extends DeObfuscators with SecurityConfiguration with UserStoreUtils {

  protected val individualUserStore: UserStore
  protected val organisationUserStore: UserStore

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val query: String => Bson = userId => equal("id", userId)

  private def getAuthorisedClientFromBson(bson: Map[String, BsonValue]): List[AuthorisedClient] = {
    bson
      .get("authorisedClients")
      .map(_.asArray().getValues.asScala.map { bson =>
        val document = bson.asDocument()
        AuthorisedClient(
          appId = document.getString("appId").getValue,
          authorisedScopes = document
            .getArray("authorisedScopes")
            .getValues
            .asScala
            .map(_.asString().getValue)
            .toSeq,
          authorisedOn = new DateTime(document.getDateTime("authorisedOn").getValue, DateTimeZone.UTC)
        )
      }.toList)
      .getOrElse(List.empty[AuthorisedClient])
  }

  def getUserInfo(userId: String)(implicit ec: ExC): Future[Option[UserInfo]] = {
    val projections = List(
      "userName",
      "digitalContact",
      "createdAt",
      "authorisedClients",
      "mfaEnabled",
      "accType",
      "profile",
      "address"
    )
    getUserStore(userId).projectValue("id", userId, projections:_*) map { data =>
      if(data.nonEmpty) {
        logger.info(s"[getUserInfo] - Found user data for user $userId")
        Some(UserInfo(
          id = data("id").asString().getValue,
          userName = stringDeObfuscate.decrypt(data("userName").asString().getValue).getOrElse("---"),
          email = stringDeObfuscate
            .decrypt(data("digitalContact").asDocument().getDocument("email").getString("address").getValue)
            .getOrElse("---"),
          emailVerified = data("digitalContact").asDocument().getDocument("email").getBoolean("verified").getValue,
          phone = data("digitalContact").asDocument().getOptionalDocument("phone").flatMap(_.getOptionalString("number")),
          phoneVerified = data("digitalContact").asDocument().getOptionalDocument("phone").flatMap(_.getOptionalBoolean("verified")).getOrElse(false),
          accType = data("accType").asString().getValue,
          name = Name(
            firstName  = data.get("profile").flatMap(_.asDocument().getOptionalString("givenName")),
            middleName = data.get("profile").flatMap(_.asDocument().getOptionalString("middleName")),
            lastName   = data.get("profile").flatMap(_.asDocument().getOptionalString("familyName")),
            nickName   = data.get("profile").flatMap(_.asDocument().getOptionalString("nickname"))
          ),
          gender = {
            val genderValue = data.get("profile").flatMap(_.asDocument().getOptionalString("gender")).getOrElse("not specified")
            Gender(
              selection = if(Gender.toList.contains(genderValue)) genderValue else "other",
              custom = if(Gender.toList.contains(genderValue)) None else Some(genderValue)
            )
          },
          birthDate = data.get("profile").flatMap(_.asDocument().getOptionalLong("birthDate").map { dateLong =>
            val date = new Date(dateLong)
            val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
            dateFormat.format(date)
          }),
          address = data.get("address").map(_.asDocument()).map { addrDoc =>
            val formatted = addrDoc.get("formatted").asString().getValue
            val streetAddress = addrDoc.get("streetAddress").asString().getValue
            val locality = addrDoc.get("locality").asString().getValue
            val region = addrDoc.get("region").asString().getValue
            val postalCode = addrDoc.get("postalCode").asString().getValue
            val country = addrDoc.get("country").asString().getValue

            Address(formatted, streetAddress, locality, region, postalCode, country)
          },
          authorisedClients = getAuthorisedClientFromBson(data),
          mfaEnabled = data("mfaEnabled").asBoolean().getValue,
          createdAt = new DateTime(
            data("createdAt").asDateTime().getValue, DateTimeZone.UTC
          )
        ))
      } else {
        logger.info(s"[getUserInfo] - Could not find data for user $userId")
        None
      }
    }
  }

  def linkClientToUser(userId: String, appId: String, scopes: Seq[String])(implicit ec: ExC): Future[LinkResponse] = {
    val update: List[AuthorisedClient] => Bson = clients => set("authorisedClients", clients)
    val collection = getUserStore(userId)

    collection.findUser(query(userId)) flatMap {
      case Some(user) =>
        val client = user.authorisedClients
          .find(_.appId == appId)
          .getOrElse(AuthorisedClient(appId, scopes, authorisedOn = DateTime.now()))
          .copy(authorisedScopes = scopes)

        val removedClient = user.authorisedClients.filterNot(_.appId == appId)
        val authorisedClients = removedClient ++ Seq(client)

        collection.updateUser(query(user.id), update(authorisedClients)) map {
          case MongoSuccessUpdate =>
            logger.info(s"[linkClientToUser] - Successfully linked $appId to user $userId")
            LinkSuccess
          case MongoFailedUpdate =>
            logger.warn(s"[linkClientToUser] - There was a problem linking $appId to user $userId")
            LinkFailed
        }
        Future.successful(LinkSuccess)
      case None =>
        logger.warn(s"[linkClientToUser] - Failed to link client to user; user not found")
        Future.successful(NoUserFound)
    }
  }

  def unlinkClientFromUser(userId: String, appId: String)(implicit ec: ExC): Future[LinkResponse] = {
    val collection = getUserStore(userId)
    val update: List[AuthorisedClient] => Bson = clients => set("authorisedClients", clients)
    collection.findUser(query(userId)) flatMap {
      case Some(user) => collection.updateUser(query(user.id), update(user.authorisedClients.filterNot(_.appId == appId))) map {
        case MongoSuccessUpdate =>
          logger.info(s"[unlinkClientFromUser] - Successfully unlinked $appId from user $userId")
          LinkSuccess
        case MongoFailedUpdate =>
          logger.warn(s"[unlinkClientFromUser] - There was a problem unlinking $appId from user $userId")
          LinkFailed
      }
      case None =>
        logger.warn(s"[unlinkClientFromUser] - Failed to unlink client to user; user not found")
        Future.successful(NoUserFound)
    }
  }

  def setEmailVerifiedStatus(userId: String, verified: Boolean)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val collection = getUserStore(userId)
    val update: Boolean => Bson = verified => set("emailVerified", verified)
    collection.updateUser(query(userId), update(verified)) map {
      case MongoSuccessUpdate =>
        logger.info(s"[setEmailVerifiedStatus] - Set email verification status to $verified for user $userId")
        MongoSuccessUpdate
      case MongoFailedUpdate =>
        logger.warn(s"[setEmailVerifiedStatus] - Failed to set email verification status for user $userId")
        MongoFailedUpdate
    }
  }

  def updateUserEmailAddress(userId: String, emailAddress: String)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val collection = getUserStore(userId)
    val update: String => Bson = email => and(
      set("email", email),
      set("emailVerified", false)
    )

    collection.updateUser(query(userId), update(emailAddress)) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateUserEmailAddress] - Updated email address for user $userId")
        MongoSuccessUpdate
      case MongoFailedUpdate =>
        logger.warn(s"[updateUserEmailAddress] - Failed to update email address for user $userId")
        MongoFailedUpdate
    }
  }

  def updatePassword(userId: String, password: String, salt: String)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val collection = getUserStore(userId)
    val update = and(
      set("password", password),
      set("salt", salt)
    )

    collection.updateUser(query(userId), update) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updatePassword] - Updated password and salt for user $userId")
        MongoSuccessUpdate
      case MongoFailedUpdate =>
        logger.warn(s"[updatePassword] - Failed to update password and salt for user $userId")
        MongoFailedUpdate
    }
  }

  def updateName(userId: String, firstName: Option[String], middleName: Option[String], lastName: Option[String], nickName: Option[String])(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val collection = getUserStore(userId)
    val update = and(
      firstName.fold(unset("profile.givenName"))(fn => set("profile.givenName", fn)),
      middleName.fold(unset("profile.middleName"))(mN => set("profile.middleName", mN)),
      lastName.fold(unset("profile.familyName"))(lN => set("profile.familyName", lN)),
      nickName.fold(unset("profile.nickname"))(lN => set("profile.nickname", lN)),
      {
        val name = s"${firstName.getOrElse("")} ${middleName.getOrElse("")} ${lastName.getOrElse("")}".trim
        if(name == "") unset("profile.name") else set("profile.name", name)
      }
    )

    collection.updateUser(query(userId), update) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateName] - Updated names for user $userId")
        MongoSuccessUpdate
      case MongoFailedUpdate =>
        logger.warn(s"[updateName] - Failed to update names for user $userId")
        MongoFailedUpdate
    }
  }

  def updateGender(userId: String, gender: String)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val collection = getUserStore(userId)
    val update = if(gender != "not specified") set("profile.gender", gender) else unset("profile.gender")

    collection.updateUser(query(userId), update) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateGender] - Updated gender for user $userId")
        MongoSuccessUpdate
      case MongoFailedUpdate =>
        logger.warn(s"[updateGender] - Failed to update gender for user $userId")
        MongoFailedUpdate
    }
  }

  def updateBirthday(userId: String, birthday: Option[Date])(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val collection = getUserStore(userId)
    val update = birthday.fold(unset("profile.birthDate"))(date => set("profile.birthDate", date))

    collection.updateUser(query(userId), update) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateBirthday] - Updated birthday for user $userId")
        MongoSuccessUpdate
      case MongoFailedUpdate =>
        logger.warn(s"[updateBirthday] - Failed to update birthday for user $userId")
        MongoFailedUpdate
    }
  }

  def updateAddress(userId: String, address: Option[Address])(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val collection = getUserStore(userId)
    val update = address.fold(unset("address"))(adr => set("address", adr))

    collection.updateUser(query(userId), update) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateAddress] - Updated address for user $userId")
        MongoSuccessUpdate
      case MongoFailedUpdate =>
        logger.warn(s"[updateAddress] - Failed to update address for user $userId")
        MongoFailedUpdate
    }
  }

  def setVerifiedPhoneNumber(userId: String, phoneNumber: String)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val collection = getUserStore(userId)
    val update = and(
      set("digitalContact.phone.number", phoneNumber),
      set("digitalContact.phone.verified", true)
    )

    collection.updateUser(query(userId), update) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateAddress] - Updated address for user $userId")
        MongoSuccessUpdate
      case MongoFailedUpdate =>
        logger.warn(s"[updateAddress] - Failed to update address for user $userId")
        MongoFailedUpdate
    }
  }

  def validateCurrentPassword(userId: String, currentPassword: String)(implicit ec: ExC): Future[Boolean] = {
    val collection = getUserStore(userId)

    collection.projectValue("id", userId, "salt", "password") map { data =>
      val salt = data("salt").asString().getValue
      val actualPassword = data("password").asString().getValue

      val hashedCurrentPassword = StringUtils.hasher(salt, currentPassword)

      val isMatched = hashedCurrentPassword == actualPassword
      logger.info(s"[validateCurrentPassword] - The supplied password did match what is on file for user $userId")
      isMatched
    }
  }
}
