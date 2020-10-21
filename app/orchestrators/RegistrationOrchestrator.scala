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

import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoSuccessCreate}
import com.cjwwdev.security.deobfuscation.DeObfuscators
import javax.inject.Inject
import models.{RegisteredApplication, User}
import org.slf4j.LoggerFactory
import services.{EmailService, RegistrationService}

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait UserRegistrationResponse
case object Registered extends UserRegistrationResponse
case object AccountIdsInUse extends UserRegistrationResponse
case object RegistrationError extends UserRegistrationResponse

sealed trait AppRegistrationResponse
case object AppRegistered extends AppRegistrationResponse
case object AppRegistrationError extends AppRegistrationResponse

class DefaultRegistrationOrchestrator @Inject()(val registrationService: RegistrationService,
                                                val emailService: EmailService) extends RegistrationOrchestrator {
  override val locale: String = ""
}

trait RegistrationOrchestrator extends DeObfuscators {

  protected val registrationService: RegistrationService
  protected val emailService: EmailService

  override val logger = LoggerFactory.getLogger(this.getClass)

  def registerUser(user: User)(implicit ec: ExC): Future[UserRegistrationResponse] = {
    registrationService.isIdentifierInUse(user.email, user.userName) flatMap {
      case true =>
        logger.warn(s"[registerUser] - Aborting registration; either email or username are already in use")
        Future.successful(AccountIdsInUse)
      case false => registrationService.validateSalt(user.salt) flatMap { saltToUse =>
        registrationService.createNewUser(user.copy(salt = saltToUse)) flatMap {
          case MongoSuccessCreate =>
            val emailAddr = stringDeObfuscate.decrypt(user.email).getOrElse(throw new Exception("Decryption error"))
            emailService.saveVerificationRecord(user.id, user.email, user.accType) map { record =>
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
}
