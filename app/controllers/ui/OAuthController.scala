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

import controllers.actions.AuthenticatedAction
import javax.inject.Inject
import orchestrators._
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import views.html.auth.Grant

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultOAuthController @Inject()(val controllerComponents: ControllerComponents,
                                       val tokenOrchestrator: TokenOrchestrator,
                                       val grantOrchestrator: GrantOrchestrator,
                                       val userOrchestrator: UserOrchestrator) extends OAuthController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait OAuthController extends BaseController with AuthenticatedAction {

  protected val grantOrchestrator: GrantOrchestrator
  protected val tokenOrchestrator: TokenOrchestrator

  implicit val ec: ExC

  private val logger = LoggerFactory.getLogger(this.getClass)

  private implicit val issuedWriter: Writes[Issued] = (issued: Issued) => {
    val idToken = issued.idToken.fold(Json.obj())(id => Json.obj("id_token" -> id))
    val refreshToken = issued.refreshToken.fold(Json.obj())(refresh => Json.obj("refresh_token" -> refresh))


    Json.obj(
      "token_type" -> issued.tokenType,
      "scope" -> issued.scope,
      "expires_in" -> issued.expiresIn,
      "access_token" -> issued.accessToken,
    ) ++ idToken ++ refreshToken
  }

  def getToken(): Action[AnyContent] = Action.async { implicit req =>
    val params = req.body.asFormUrlEncoded.getOrElse(Map())
    val grantType = params("grant_type").headOption.getOrElse("")

    logger.info(s"[getToken] - Attempting to issue tokens using the $grantType grant")
    grantType match {
      case "authorization_code" =>
        val authCode = params("code").headOption.getOrElse("")
        val clientId = params("client_id").headOption.getOrElse("")
        val redirectUri = params("redirect_uri").headOption.getOrElse("")
        val codeVerifier = params.getOrElse("code_verifier", Seq()).headOption
        tokenOrchestrator.authorizationCodeGrant(authCode, clientId, redirectUri, codeVerifier) map {
          case iss@Issued(_,_,_,_,_,_) => Ok(Json.toJson(iss))
          case resp => BadRequest(Json.obj("error" -> resp.toString))
        }
      case "client_credentials" =>
        val scope = params("scope").headOption.getOrElse("")
        tokenOrchestrator.clientCredentialsGrant(scope) map {
          case iss@Issued(_,_,_,_,_,_) => Ok(Json.toJson(iss))
          case resp => BadRequest(Json.obj("error" -> resp.toString))
        }
      case "refresh_token" =>
        val refreshToken = params("refresh_token").headOption.getOrElse("")
        tokenOrchestrator.refreshTokenGrant(refreshToken) map {
          case iss@Issued(_,_,_,_,_,_) => Ok(Json.toJson(iss))
          case resp => BadRequest(Json.obj("error" -> resp.toString))
        }
      case e =>
        logger.error(s"[issueToken] - Could not validate grant type $e")
        Future.successful(BadRequest(Json.obj("error" -> InvalidGrantType.toString)))
    }
  }

  def authoriseGet(response_type: String, client_id: String, scope: String, state: Option[String], code_verifier: Option[String], code_challenge: Option[String], code_challenge_method: Option[String]): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    grantOrchestrator.validateIncomingGrant(response_type, client_id, scope, userId) flatMap {
      case ValidatedGrantRequest(app, scopes) => Future.successful(Ok(Grant(response_type, client_id, Seq(), scopes, scope, app, state, code_verifier, code_challenge, code_challenge_method)))
      case PreviouslyAuthorised => authorisePost(response_type, client_id, scope, state, code_verifier, code_challenge, code_challenge_method)(req)
      case ScopeDrift(app, authScopes, reqScopes) => Future.successful(Ok(Grant(response_type, client_id, authScopes, reqScopes, scope, app, state, code_verifier, code_challenge, code_challenge_method)))
      case err => Future.successful(BadRequest(err.toString))
    }
  }

  def authorisePost(response_type: String, client_id: String, scope: String, state: Option[String], code_verifier: Option[String], code_challenge: Option[String], code_challenge_method: Option[String]): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    val scopes = scope.trim.split(" ").toSeq

    grantOrchestrator.saveIncomingGrant(response_type, client_id, userId, scopes, code_verifier, code_challenge, code_challenge_method) map {
      case Some(grant) => Redirect(
        grant.redirectUri,
        Map("code" -> Seq(grant.authCode)) ++ state.fold[Map[String, Seq[String]]](Map())(ste => Map("state" -> Seq(ste)))
      )
      case None => BadRequest
    }
  }
}
