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

import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoFailedUpdate, MongoSuccessCreate, MongoSuccessUpdate}
import com.cjwwdev.security.deobfuscation.DeObfuscators
import javax.inject.Inject
import models.{RegisteredApplication, User, Verification}
import org.slf4j.LoggerFactory
import play.api.mvc.Request
import services.{EmailService, PhoneService, RegistrationService, UserService}

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait UserRegistrationResponse
case object Registered extends UserRegistrationResponse
case object AccountIdsInUse extends UserRegistrationResponse
case object RegistrationError extends UserRegistrationResponse

sealed trait AppRegistrationResponse
case object AppRegistered extends AppRegistrationResponse
case object AppRegistrationError extends AppRegistrationResponse

sealed trait VerificationResponse
case object EmailVerified extends VerificationResponse
case object PhoneVerified extends VerificationResponse
case object ErrorRetryAllowed extends VerificationResponse
case object NoRecordFound extends VerificationResponse
case object NoUserFound extends VerificationResponse
case object VerificationSent extends VerificationResponse

class DefaultRegistrationOrchestrator @Inject()(val registrationService: RegistrationService,
                                                val userService: UserService,
                                                val phoneService: PhoneService,
                                                val emailService: EmailService) extends RegistrationOrchestrator {
  override val locale: String = ""
}

trait RegistrationOrchestrator extends DeObfuscators {

  protected val registrationService: RegistrationService
  protected val emailService: EmailService
  protected val userService: UserService
  protected val phoneService: PhoneService

  override val logger = LoggerFactory.getLogger(this.getClass)

  def registerUser(user: User)(implicit ec: ExC, req: Request[_]): Future[UserRegistrationResponse] = {
    registrationService.isIdentifierInUse(user.digitalContact.email.address, user.userName) flatMap {
      case true =>
        logger.warn(s"[registerUser] - Aborting registration; either email or username are already in use")
        Future.successful(AccountIdsInUse)
      case false => registrationService.validateSalt(user.salt) flatMap { saltToUse =>
        registrationService.createNewUser(user.copy(salt = saltToUse)) flatMap {
          case MongoSuccessCreate =>
            val emailAddr = stringDeObfuscate.decrypt(user.digitalContact.email.address).getOrElse(throw new Exception("Decryption error"))
            emailService.saveVerificationRecord(user.id, user.digitalContact.email.address, user.accType) map { record =>
              emailService.sendEmailVerificationMessage(emailAddr, record)
              logger.info(s"[registerUser] - Registration successful; new user under ${user.id}")
              Registered
            }
          case MongoFailedCreate =>
            logger.error(s"[registerUser] - Registration unsuccessful; There was a problem creating the user")
            Future.successful(RegistrationError)
        }
      }
    }
  }

  def registerApplication(app: RegisteredApplication)(implicit ec: ExC): Future[AppRegistrationResponse] = {
    registrationService.validateIdsAndSecrets(app) flatMap { cleansedApp =>
      registrationService.createApp(cleansedApp) map {
        case MongoSuccessCreate => AppRegistered
        case MongoFailedCreate  => AppRegistrationError
      }
    }
  }

  def confirmEmailAddress(verificationRecord: Verification)(implicit ec: ExC): Future[VerificationResponse] = {
    emailService.validateVerificationRecord(verificationRecord) flatMap {
      case Some(rec) => userService.setEmailVerifiedStatus(rec.userId, verified = true) flatMap {
        case MongoSuccessUpdate =>
          logger.info(s"[confirmEmailAddress] - Email for ${rec.userId} is now verified")
          emailService.removeVerificationRecord(rec.verificationId) map {
            _ => EmailVerified
          }
        case MongoFailedUpdate =>
          logger.warn(s"[confirmEmailAddress] - Failed to set the verification status for user ${rec.userId}; retry is allowed")
          Future.successful(ErrorRetryAllowed)
      }
      case None =>
        logger.warn(s"[confirmEmailAddress] - No verification record found for ${verificationRecord.verificationId}")
        Future.successful(NoRecordFound)
    }
  }

  def sendPhoneVerificationMessage(userId: String, phoneNumber: String)(implicit ec: ExC): Future[VerificationResponse] = {
    userService.getUserInfo(userId) flatMap {
      case Some(userInfo) => phoneService.saveVerificationRecord(userId, phoneNumber, userInfo.accType) map { verification =>
        phoneService.sendSMSVerification(verification.contact, verification.code.get)
        VerificationSent
      }
      case None =>
        logger.warn(s"[sendPhoneVerificationMessage] - No user found for $userId")
        Future.successful(NoUserFound)
    }
  }

  def verifySentCode(userId: String, verificationCode: String)(implicit ec: ExC): Future[VerificationResponse] = {
    userService.getUserInfo(userId) flatMap {
      case Some(userInfo) => phoneService.validateVerificationRecord(userInfo.id, verificationCode) flatMap {
        case Some(record) => userService.setVerifiedPhoneNumber(record.userId, record.contact) map {
          _ => PhoneVerified
        }
        case None =>
          logger.warn(s"[verifySentCode] - No verification record found")
          Future.successful(NoRecordFound)
      }
      case None =>
        logger.warn(s"[verifySentCode] - No user found")
        Future.successful(NoUserFound)
    }
  }
}
