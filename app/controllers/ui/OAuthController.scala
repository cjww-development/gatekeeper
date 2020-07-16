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

package controllers.ui

import javax.inject.Inject
import models.{AuthorisationRequest, Grant => GrantModel}
import orchestrators.{GrantOrchestrator, Issued, TokenOrchestrator, ValidatedGrantRequest}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import views.html.auth.Grant
import models.ServerCookies._

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultOAuthController @Inject()(val controllerComponents: ControllerComponents,
                                       val tokenOrchestrator: TokenOrchestrator,
                                       val grantOrchestrator: GrantOrchestrator) extends OAuthController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait OAuthController extends BaseController {

  protected val grantOrchestrator: GrantOrchestrator
  protected val tokenOrchestrator: TokenOrchestrator

  implicit val ec: ExC

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getToken(grant_type: String, code: String, redirect_uri: String, client_id: String, client_secret: Option[String]): Action[AnyContent] = {
    Action.async { implicit req =>
      tokenOrchestrator.issueToken(code) map {
        case Issued(token) => Ok(Json.parse(s"""{ "access_token" : "$token" }"""))
        case _ => BadRequest
      }
    }
  }

  def authorise(): Action[AnyContent] = ???

  def createGrant(): Action[AnyContent] = Action.async { implicit req =>
    val authReq = AuthorisationRequest(
      responseType = req.queryString("response_type").head,
      clientId = req.queryString("client_id").head,
      redirectUri = req.queryString("redirect_uri").head,
      scope = req.queryString("scope").map(_.trim.split(",")).flatten,
      state = req.queryString("state").head
    )
    grantOrchestrator.initiateGrantRequest(authReq) map {
      case ValidatedGrantRequest(app, scope) => Ok(Grant(app, scope, authReq))
      case err => BadRequest(err.toString)
    }
  }

  def submitGrant(response_type: String, client_id: String, redirect_uri: String, scope: String, state: String): Action[AnyContent] = Action.async { implicit req =>
    val authReq = AuthorisationRequest(
      responseType = response_type,
      clientId = client_id,
      redirectUri = redirect_uri,
      scope = scope.split(",").toSeq,
      state = state
    )

    req.cookies.get("aas") match {
      case Some(cookie) => grantOrchestrator.saveGrant(authReq, cookie.getValue()) map { grant =>
        Ok(Json.toJson(grant)(GrantModel.outboundWriter).as[JsObject] ++ Json.obj("state" -> state))
      }
      case None => Future.successful(Forbidden)
    }
  }
}
