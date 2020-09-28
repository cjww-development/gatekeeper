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
import models.UserInfo
import org.slf4j.LoggerFactory
import services.UserService

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultUserOrchestrator @Inject()(val userService: UserService) extends UserOrchestrator

trait UserOrchestrator {

  protected val userService: UserService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getUserDetails(id: String)(implicit ec: ExC): Future[Option[UserInfo]] = {
    def invalidUser(): Option[UserInfo] = {
      logger.warn(s"[getUserDetails] - Invalid userId $id")
      None
    }

    userService.getUserInfo(id).map(user => if(user.nonEmpty) user else invalidUser())
  }
}
