/*
 * Copyright 2019 CJWW Development
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

package controllers

import com.cjwwdev.featuremanagement.services.FeatureService
import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoSuccessCreate}
import com.cjwwdev.security.sha.SHA512
import javax.inject.Inject
import models.{AuthCodeRequest, User}
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Cookie}
import services.{AuthService, UserService}

import scala.concurrent.{ExecutionContext, Future}

class DefaultUserController @Inject()(val controllerComponents: ControllerComponents,
                                      val featureService: FeatureService,
                                      val userService: UserService,
                                      val authService: AuthService) extends UserController {
  override implicit val ec: ExecutionContext = controllerComponents.executionContext
}

trait UserController extends BackendController {

  val userService: UserService
  val authService: AuthService

  def createUser(): Action[JsValue] = Action.async(parse.json) { implicit req =>
    withJsonBody[User] { user =>
      userService.createUser(user) map { res =>
        val (status, body) = res match {
          case MongoSuccessCreate => (CREATED, JsString(s"User create"))
          case MongoFailedCreate  => (BAD_REQUEST, JsString(s"User was not created"))
        }

        withJsonResponseBody(status, body) {
          json => Status(status)(json)
        }
      }
    }
  }

  def login(): Action[JsValue] = Action.async(parse.json) { implicit req =>
    val userName = req.body.\("username").as[String]
    val password = SHA512.encrypt(req.body.\("password").as[String])

    userService.login(userName, password) map { user =>
      if(user.nonEmpty) {
        Redirect(controllers.routes.UserController.authed())
          .withCookies(Cookie(
            name = "authed-as",
            value = user.get.email
          ))
      } else {
        Forbidden(Json.parse("""{ "blocked" : "no user" }"""))
      }
    }
  }

  def authed(): Action[AnyContent] = Action.async { implicit req =>
    val authed = req.cookies.get("authed-as")
    if(authed.nonEmpty) {
      Future.successful(Ok(Json.parse("""{ "hello" : "authed" }""")))
    } else {
      Future.successful(Forbidden(Json.parse("""{ "blocked" : "blocked" }""")))
    }
  }

  def grantAccessToUser(): Action[AnyContent] = Action.async { implicit req =>
    val responseType = req.queryString.get("response_type")
    val clientId     = req.queryString.get("client_id")
    val redirectUri  = req.queryString.get("redirect_uri")
    val scope        = req.queryString.get("scope")
    val state        = req.queryString.get("state")

    val authCodeReq = AuthCodeRequest(
      responseType = responseType.map(_.head).getOrElse(""),
      clientId = clientId.map(_.head).getOrElse(""),
      redirectUri = redirectUri.map(_.head).getOrElse(""),
      scope = scope.map(_.head).getOrElse(""),
      state = state.map(_.head).getOrElse("")
    )

    authService.generateAuthCode(authCodeReq) map { authCode =>
      Redirect(s"${authCodeReq.redirectUri}?code=${authCode.code}&state=${authCode.state}")
    }
  }

  def issueToken(): Action[AnyContent] = Action.async { implicit req =>
    val code = req.queryString("code").head
    val state = req.queryString("state").head

    req.cookies.get("authed-as") match {
      case Some(cookie) => authService.issueToken(cookie.value, code, state) map {
        case Right(token) => Ok(Json.parse(s"""{ "token" : "$token" }"""))
        case Left(_) => Forbidden("Code or state mismatch")
      }
      case None => Future.successful(Forbidden("Not logged in"))
    }
  }
}
