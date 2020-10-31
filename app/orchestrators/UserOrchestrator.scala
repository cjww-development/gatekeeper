/*
 * Copyright 2020 CJWW Development
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

import com.cjwwdev.security.obfuscation.Obfuscators
import javax.inject.Inject
import models.{ChangeOfPassword, UserInfo}
import org.slf4j.LoggerFactory
import play.api.mvc.Request
import services.{EmailService, RegistrationService, UserService}
import utils.StringUtils

import scala.concurrent.{Future, ExecutionContext => ExC}

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

trait UserOrchestrator extends Obfuscators {

  override val locale: String = ""

  protected val userService: UserService
  protected val emailService: EmailService
  protected val registrationService: RegistrationService

  override val logger = LoggerFactory.getLogger(this.getClass)

  def getUserDetails(id: String)(implicit ec: ExC): Future[Option[UserInfo]] = {
    def invalidUser(): Option[UserInfo] = {
      logger.warn(s"[getUserDetails] - Invalid userId $id")
      None
    }

    userService.getUserInfo(id).map(user => if(user.nonEmpty) user else invalidUser())
  }

  def updateEmailAndReverify(userId: String, email: String)(implicit ec: ExC, req: Request[_]): Future[UserUpdateResponse] = {
    userService.getUserInfo(userId) flatMap {
      case Some(user) => if(user.email != email) {
        val obsEmail = stringObs.encrypt(email)
        registrationService.isIdentifierInUse(obsEmail, "") flatMap { inUse =>
          if(inUse) {
            logger.error(s"[updateEmailAndReverify] - The email requested is already in use")
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
        logger.warn(s"[updateEmailAndReverify] - Aborting update, email address for user $userId has not changed")
        Future.successful(NoUpdateRequired)
      }
      case None =>
        logger.error(s"[updateEmailAndReverify] - Failed updating email, no user found for userId $userId")
        Future.successful(NoUser)
    }
  }

  def updatePassword(userId: String, changeOfPassword: ChangeOfPassword)(implicit ec: ExC): Future[UserUpdateResponse] = {
    if(changeOfPassword.newPassword == changeOfPassword.confirmedPassword) {
      userService.validateCurrentPassword(userId, changeOfPassword.oldPassword) flatMap { isMatched =>
        if(isMatched) {
          val salt = StringUtils.salter(length = 32)
          for {
            saltToUse <- registrationService.validateSalt(salt)
            _ <- userService.updatePassword(userId, StringUtils.hasher(saltToUse, changeOfPassword.newPassword), saltToUse)
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
}
