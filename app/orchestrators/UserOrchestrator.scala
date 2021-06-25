/*
 * Copyright 2021 CJWW Development
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

package orchestrators

import dev.cjww.mongo.responses.MongoUpdatedResponse
import models._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import services.comms.EmailService
import services.users.{RegistrationService, UserService}
import utils.StringUtils._

import java.text.SimpleDateFormat
import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.util.Try

sealed trait UserUpdateResponse
case object NoUpdateRequired extends UserUpdateResponse
case object NoUser extends UserUpdateResponse
case object EmailUpdated extends UserUpdateResponse
case object EmailInUse extends UserUpdateResponse
case object PasswordMismatch extends UserUpdateResponse
case object PasswordUpdated extends UserUpdateResponse
case object InvalidOldPassword extends UserUpdateResponse

class DefaultUserOrchestrator @Inject()(val userService: UserService,
                                        val emailService: EmailService,
                                        val registrationService: RegistrationService) extends UserOrchestrator

trait UserOrchestrator {

  protected val userService: UserService
  protected val emailService: EmailService
  protected val registrationService: RegistrationService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getUserDetails(id: String)(implicit ec: ExC): Future[Option[UserInfo]] = {
    def invalidUser(): Option[UserInfo] = {
      logger.warn(s"[getUserDetails] - Invalid userId $id")
      None
    }

    userService.getUserInfo(id).map(user => if(user.nonEmpty) user else invalidUser())
  }

  def updateEmailAndReVerify(userId: String, email: String)(implicit ec: ExC, req: Request[_]): Future[UserUpdateResponse] = {
    userService.getUserInfo(userId) flatMap {
      case Some(user) => if(user.email != email) {
        val obsEmail = email.encrypt
        registrationService.isIdentifierInUse(obsEmail, "") flatMap { inUse =>
          if(inUse) {
            logger.error(s"[updateEmailAndReVerify] - The email requested is already in use")
            Future.successful(EmailInUse)
          } else {
            for {
              _ <- userService.updateUserEmailAddress(userId, obsEmail)
              vRec <- emailService.saveVerificationRecord(userId, obsEmail, user.accType)
            } yield {
              emailService.sendEmailVerificationMessage(email, vRec)
              EmailUpdated
            }
          }
        }
      } else {
        logger.warn(s"[updateEmailAndReVerify] - Aborting update, email address for user $userId has not changed")
        Future.successful(NoUpdateRequired)
      }
      case None =>
        logger.error(s"[updateEmailAndReVerify] - Failed updating email, no user found for userId $userId")
        Future.successful(NoUser)
    }
  }

  def updatePassword(userId: String, changeOfPassword: ChangeOfPassword)(implicit ec: ExC): Future[UserUpdateResponse] = {
    if(changeOfPassword.newPassword == changeOfPassword.confirmedPassword) {
      userService.validateCurrentPassword(userId, changeOfPassword.oldPassword) flatMap { isMatched =>
        if(isMatched) {
          val salt = salter(length = 32)
          for {
            saltToUse <- registrationService.validateSalt(salt)
            _ <- userService.updatePassword(userId, hasher(saltToUse, changeOfPassword.newPassword), saltToUse)
          } yield {
            logger.info(s"[updatePassword] - Password for user $userId has been updated")
            PasswordUpdated
          }
        } else {
          logger.warn(s"[updatePassword] - The current password for user $userId did not match")
          Future.successful(InvalidOldPassword)
        }
      }
    } else {
      logger.error(s"[updatePassword] - The supplied passwords do not match for user $userId")
      Future.successful(PasswordMismatch)
    }
  }

  def updateName(userId: String, name: Name)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    userService.updateName(userId, name.firstName, name.middleName, name.lastName, name.nickName)
  }

  def updateGender(userId: String, gender: Gender)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val genderToSave = gender.custom.getOrElse(gender.selection)
    userService.updateGender(userId, genderToSave)
  }

  def updateAddress(userId: String, address: Address)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    userService.updateAddress(userId, if(address.isEmpty) None else Some(address))
  }

  def updateBirthday(userId: String, birthday: String)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val date = Try(new SimpleDateFormat("yyyy-MM-dd").parse(birthday)).fold(e => {e.printStackTrace(); None}, dte => Some(dte))
    userService.updateBirthday(userId, date)
  }

  def getScopedUserInfo(userId: String, scopes: Seq[String])(implicit ec: ExC): Future[JsObject] = {
    val scopeMethods: Map[String, UserInfo => JsObject] = Map(
      "openid"  -> UserInfo.toOpenId,
      "profile" -> UserInfo.toProfile,
      "email"   -> UserInfo.toEmail,
      "address" -> UserInfo.toAddress,
      "phone"   -> UserInfo.toPhone,
    )

    userService.getUserInfo(userId) map {
      case Some(userInfo) => scopes.map(scp => scopeMethods(scp)(userInfo)).fold(Json.obj())((a, b) => a ++ b)
      case None => Json.obj()
    }
  }
}
