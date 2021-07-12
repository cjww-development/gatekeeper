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

package controllers.api

import controllers.actions.OAuthAction
import orchestrators.UserOrchestrator
import play.api.Configuration
import play.api.mvc._
import services.oauth2.TokenService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext => ExC}

class DefaultAccountController @Inject()(val controllerComponents: ControllerComponents,
                                         val config: Configuration,
                                         val tokenService: TokenService,
                                         val userOrchestrator: UserOrchestrator) extends AccountController {
  override implicit val ec: ExC = controllerComponents.executionContext
  override val signature: String = config.get[String]("jwt.signature")
}

trait AccountController extends BaseController with OAuthAction {

  val signature: String

  protected val userOrchestrator: UserOrchestrator

  implicit val ec: ExC

  def getUserDetails: Action[AnyContent] = authorised { _ => userId => scopes =>
    userOrchestrator.getScopedUserInfo(userId, scopes) map {
      json => Ok(json)
    }
  }
}
