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

import controllers.actions.BasicAuthAction
import errors.StandardErrors
import orchestrators.TokenOrchestrator
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.ClientService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultRevokationController @Inject()(val controllerComponents: ControllerComponents,
                                            val tokenOrchestrator: TokenOrchestrator,
                                            val clientService: ClientService) extends RevokationController {
  override implicit val ec: ExecutionContext = controllerComponents.executionContext
}

trait RevokationController extends BaseController with BasicAuthAction {

  val tokenOrchestrator: TokenOrchestrator

  def revokeToken(): Action[AnyContent] = clientAuthentication { implicit req => _ => _ =>
    val body = req.body.asFormUrlEncoded.getOrElse(Map())

    val token = body.get("token").map(_.head)
    val tokenType = body.get("token_type").map(_.head)

    if(token.isDefined) {
      tokenOrchestrator.revokeToken(token.get, tokenType) map {
        _ => Ok
      }
    } else {
      Future.successful(BadRequest(StandardErrors.INVALID_REQUEST))
    }
  }
}
