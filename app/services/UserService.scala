package services

import com.cjwwdev.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate, MongoUpdatedResponse}
import com.cjwwdev.security.SecurityConfiguration
import com.cjwwdev.security.deobfuscation.DeObfuscators
import database.{UserStore, UserStoreUtils}
import javax.inject.{Inject, Named}
import models.{AuthorisedClient, UserInfo}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import org.slf4j.{Logger, LoggerFactory}

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
    val projections = List("userName", "email", "emailVerified", "createdAt", "authorisedClients", "mfaEnabled", "accType")
    getUserStore(userId).projectValue("id", userId, projections:_*) map { data =>
      if(data.nonEmpty) {
        logger.info(s"[getUserInfo] - Found user data for user $userId")
        Some(UserInfo(
          id = data("id").asString().getValue,
          userName = stringDeObfuscate.decrypt(data("userName").asString().getValue).getOrElse("---"),
          email = stringDeObfuscate.decrypt(data("email").asString().getValue).getOrElse("---"),
          emailVerified = data("emailVerified").asBoolean().getValue,
          accType = data("accType").asString().getValue,
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
}
