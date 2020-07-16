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

import javax.inject.Inject
import models.{Login, User}
import org.slf4j.LoggerFactory
import services.LoginService

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultLoginOrchestrator @Inject()(val loginService: LoginService) extends LoginOrchestrator

trait LoginOrchestrator {

  protected val loginService: LoginService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def authenticateUser(loginAttempt: Login)(implicit ec: ExC): Future[Option[User]] = {
    loginService.getUserSalt(loginAttempt.accountId).flatMap {
      case Some(salt) =>
        val hashedLoginAttempt = Login.apply(loginAttempt.accountId, salt, loginAttempt.password)
        loginService.validateUser(hashedLoginAttempt.accountId, hashedLoginAttempt.password)
      case None =>
        logger.warn("[authenticateUser] - No salt found for user, aborting login attempt")
        Future.successful(None)
    }
  }
}
