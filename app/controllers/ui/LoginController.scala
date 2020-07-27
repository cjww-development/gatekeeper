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

import forms.LoginForm.{form => loginForm, _}
import javax.inject.Inject
import models.{ServerCookies, User}
import orchestrators.LoginOrchestrator
import org.slf4j.LoggerFactory
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import controllers.ui.routes
import views.html.login.Login

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultLoginController @Inject()(val controllerComponents: ControllerComponents,
                                       val loginOrchestrator: LoginOrchestrator) extends LoginController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait LoginController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport {

  protected val loginOrchestrator: LoginOrchestrator

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  private val logger = LoggerFactory.getLogger(this.getClass)

  def show(): Action[AnyContent] = Action { implicit req =>
    checkCookies(
      block    = Redirect(routes.AccountController.show()),
      continue = Ok(Login(loginForm))
    )
  }

  def submit(): Action[AnyContent] = Action.async { implicit req =>
    val redirect = req.body.asFormUrlEncoded.flatMap(_.get("redirect").map(_.head))
    loginForm.bindFromRequest.fold(
      err   => Future.successful(BadRequest(Login(loginForm.renderErrors))),
      login => loginOrchestrator.authenticateUser(login) map {
        case Some(user) => loginRedirect(user, redirect)
        case None       => BadRequest(Login(loginForm.renderErrors))
      }
    )
  }

  private def loginRedirect(user: User, redirect: Option[String]): Result = {
    Redirect(Call("GET", redirect.getOrElse(routes.AccountController.show().url)))
      .withCookies(ServerCookies.createAuthCookie(user.id, enc = true))
  }

  def logout(): Action[AnyContent] = Action { implicit req =>
    Redirect(routes.LoginController.show())
      .discardingCookies(DiscardingCookie("aas"))
  }

  private def checkCookies(block: => Result, continue: => Result)(implicit req: Request[_]): Result = {
    req
      .cookies
      .get("aas")
      .fold(continue)(_ => block)
  }
}
