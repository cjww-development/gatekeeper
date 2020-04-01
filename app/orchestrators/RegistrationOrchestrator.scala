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
import javax.inject.Inject
import models.User
import org.slf4j.LoggerFactory
import services.RegistrationService

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait RegistrationResponse
case object Registered extends RegistrationResponse
case object EmailInUse extends RegistrationResponse
case object UserNameInUse extends RegistrationResponse
case object BothInUse extends RegistrationResponse
case object RegistrationError extends RegistrationResponse

class DefaultRegistrationOrchestrator @Inject()(val registrationService: RegistrationService) extends RegistrationOrchestrator

trait RegistrationOrchestrator {

  protected val registrationService: RegistrationService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def registerUser(user: User)(implicit ec: ExC): Future[RegistrationResponse] = {
    for {
      emailInUse    <- registrationService.validateEmail(user.email)
      userNameInUse <- registrationService.validateUsername(user.userName)
      registered    <- (emailInUse, userNameInUse) match {
        case (true, true)   =>
          logger.warn(s"[registerUser] - Aborting registration; both email and username are already in use")
          Future.successful(BothInUse)
        case (false, true)  =>
          logger.warn(s"[registerUser] - Aborting registration; username is already in use")
          Future.successful(UserNameInUse)
        case (true, false)  =>
          logger.warn(s"[registerUser] - Aborting registration; email is already in use")
          Future.successful(EmailInUse)
        case (false, false) => registrationService.createNewUser(user) flatMap {
          case MongoSuccessCreate =>
            logger.info(s"[registerUser] - Registration successful; new user under ${user.id}")
            Future.successful(Registered)
          case MongoFailedCreate  =>
            logger.error(s"[registerUser] - Registration unsuccessful; There was a problem creating the user")
            Future.successful(RegistrationError)
        }
      }
    } yield registered
  }
}
