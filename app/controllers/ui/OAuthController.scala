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

import controllers.actions.AuthenticatedFilter
import javax.inject.Inject
import orchestrators._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import views.html.auth.Grant

import scala.concurrent.{ExecutionContext => ExC}

class DefaultOAuthController @Inject()(val controllerComponents: ControllerComponents,
                                       val tokenOrchestrator: TokenOrchestrator,
                                       val grantOrchestrator: GrantOrchestrator,
                                       val userOrchestrator: UserOrchestrator) extends OAuthController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait OAuthController extends BaseController with AuthenticatedFilter {

  protected val grantOrchestrator: GrantOrchestrator
  protected val tokenOrchestrator: TokenOrchestrator

  implicit val ec: ExC

  private val logger = LoggerFactory.getLogger(this.getClass)

  private implicit val issuedWriter: Writes[Issued] = (issued: Issued) => Json.obj(
    "token_type" -> issued.tokenType,
    "scope" -> issued.scope,
    "expires_in" -> issued.expiresIn,
    "access_token" -> issued.accessToken,
    "id_token" -> issued.idToken
  )

  def getToken(): Action[AnyContent] = Action.async { implicit req =>
    val params = req.body.asFormUrlEncoded.getOrElse(Map())
    val grantType = params("grant_type").headOption.getOrElse("")
    val authCode = params("code").headOption.getOrElse("")
    val clientId = params("client_id").headOption.getOrElse("")
    val redirectUri = params("redirect_uri").headOption.getOrElse("")

    tokenOrchestrator.issueToken(grantType, authCode, clientId, redirectUri) map {
      case iss@Issued(_,_,_,_,_) => Ok(Json.toJson(iss))
      case resp => BadRequest(Json.obj("error" -> resp.toString))
    }
  }

  def authoriseGet(response_type: String, client_id: String, scope: String): Action[AnyContent] = authenticatedUser { implicit req => _ =>
    val scopes = scope.trim.split(",").map(_.trim).toSeq
    grantOrchestrator.validateIncomingGrant(response_type, client_id, scopes) map {
      case ValidatedGrantRequest(app, scopes) => Ok(Grant(response_type, client_id, scopes, scope, app))
      case err => BadRequest(err.toString)
    }
  }

  def authorisePost(response_type: String, client_id: String, scope: String): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    val scopes = scope.trim.split(",").toSeq

    grantOrchestrator.saveIncomingGrant(response_type, client_id, userId, scopes) map {
      case Some(grant) => Redirect(grant.redirectUri, Map(
        "code" -> Seq(grant.authCode)
      ))
      case None => BadRequest
    }
  }
}
