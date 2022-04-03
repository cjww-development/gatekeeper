/*
 * Copyright 2022 CJWW Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.users

import database.{UserStore, UserStoreUtils}
import dev.cjww.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate, MongoUpdatedResponse}
import models._
import org.joda.time.DateTime
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set, unset}
import org.slf4j.{Logger, LoggerFactory}
import utils.StringUtils

import java.util.Date
import javax.inject.{Inject, Named}
import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait LinkResponse
case object LinkSuccess extends LinkResponse
case object LinkFailed extends LinkResponse
case object LinkExists extends LinkResponse
case object LinkRemoved extends LinkResponse
case object NoUserFound extends LinkResponse

class DefaultUserService @Inject()(@Named("individualUserStore") val individualUserStore: UserStore,
                                   @Named("organisationUserStore") val organisationUserStore: UserStore) extends UserService

trait UserService extends UserStoreUtils {

  protected val individualUserStore: UserStore
  protected val organisationUserStore: UserStore

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val query: String => Bson = userId => equal("id", userId)

  def getUserInfo(userId: String)(implicit ec: ExC): Future[Option[UserInfo]] = {
    getUserStore(userId).findUser(query(userId)).map {
      _.map { user =>
        logger.info(s"[getUserInfo] - Found user data for user $userId")
        UserInfo.fromUser(user)
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
    val update: Boolean => Bson = verified => set("digitalContact.email.verified", verified)
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
    val update: String => Bson = email => combine(
      set("digitalContact.email.address", email),
      set("digitalContact.email.verified", false)
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
    val update = combine(
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

  def updateName(userId: String, firstName: Option[String], middleName: Option[String], lastName: Option[String], nickName: Option[String])
                (implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val collection = getUserStore(userId)
    val update = combine(
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
    val addressUpdate = address.fold(unset("address"))(adr => set("address", adr))

    collection.updateUser(query(userId), addressUpdate) map {
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
    val phoneNumberUpdate = combine(
      set("digitalContact.phone.number", phoneNumber),
      set("digitalContact.phone.verified", true)
    )

    collection.updateUser(query(userId), phoneNumberUpdate) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateAddress] - Updated address for user $userId")
        MongoSuccessUpdate
      case MongoFailedUpdate =>
        logger.warn(s"[updateAddress] - Failed to update address for user $userId")
        MongoFailedUpdate
    }
  }

  def validateCurrentPassword(userId: String, currentPassword: String)(implicit ec: ExC): Future[Boolean] = {
    getUserStore(userId).findUser(query(userId)).map {
      case Some(user) =>
        val salt = user.salt
        val actualPassword = user.password

        val hashedCurrentPassword = StringUtils.hasher(salt, currentPassword)

        val isMatched = hashedCurrentPassword == actualPassword
        logger.info(s"[validateCurrentPassword] - The supplied password did match what is on file for user $userId")
        isMatched
      case None =>
        logger.info(s"[validateCurrentPassword] - There was no user matching userId $userId")
        false
    }
  }
}
