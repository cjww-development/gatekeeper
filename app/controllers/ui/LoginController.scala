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

package controllers.ui

import forms.LoginForm.{form => loginForm, _}
import forms.MFAForm._
import models.ServerCookies
import models.ServerCookies.CookieOps
import orchestrators._
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import views.html.login.{Login, MFACode}

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultLoginController @Inject()(val controllerComponents: ControllerComponents,
                                       val loginOrchestrator: LoginOrchestrator) extends LoginController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait LoginController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport {

  protected val loginOrchestrator: LoginOrchestrator

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  def loginShow(): Action[AnyContent] = Action { implicit req =>
    checkCookies(
      block    = Redirect(routes.AccountController.show()),
      continue = Ok(Login(loginForm))
    )
  }

  def loginSubmit(): Action[AnyContent] = Action.async { implicit req =>
    val redirect = req.body.asFormUrlEncoded.flatMap(_.get("redirect").map(_.head))
    loginForm.bindFromRequest().fold(
      _     => Future.successful(BadRequest(Login(loginForm.renderErrors))),
      login => loginOrchestrator.authenticateUser(login) map {
        case Some(attId) => mfaRedirect(attId, redirect)
        case None        => BadRequest(Login(loginForm.renderErrors))
      }
    )
  }

  def mfaShow(): Action[AnyContent] = Action.async { implicit req =>
    val redirect = if(req.rawQueryString.trim == "") None else Some(req.rawQueryString.replace("redirect=", ""))
    checkMFACookie(
      block = Future.successful(Redirect(routes.LoginController.logout())),
      attId => loginOrchestrator.mfaChallengePresenter(attId) map {
        case TOTPMFAChallenge => Ok(MFACode(mfaForm))
        case NoMFAChallengeNeeded(userId) => loginRedirect(userId, redirect)
        case InvalidLogonAttempt => Redirect(routes.LoginController.logout())
      }
    )
  }

  def mfaSubmit(): Action[AnyContent] = Action.async { implicit req =>
    val redirect = req.body.asFormUrlEncoded.flatMap(_.get("redirect").map(_.head))
    checkMFACookie(
      block = Future.successful(Redirect(routes.LoginController.logout())),
      attId => mfaForm.bindFromRequest().fold(
        _    => Future.successful(BadRequest(MFACode(mfaForm.renderErrors))),
        code => loginOrchestrator.verifyMFAChallenge(attId, code.code) map {
          case MFAValidated(id) => loginRedirect(id, redirect)
          case MFAInvalid => BadRequest(MFACode(mfaForm.renderInvalidErrors))
          case MFAError => throw new Exception
        }
      )
    )
  }

  private def mfaRedirect(attId: String, redirect: Option[String]): Result = {
    val redirectTo = redirect.fold(routes.LoginController.mfaShow().url) {
      url => if(url.trim == "") routes.LoginController.mfaShow().url else s"${routes.LoginController.mfaShow().url}?redirect=$url"
    }

    Redirect(Call("GET", redirectTo))
      .withCookies(ServerCookies.createMFAChallengeCookie(attId, enc = true))
  }

  private def loginRedirect(userId: String, redirect: Option[String]): Result = {
    val redirectTo = redirect.fold(routes.AccountController.show().url) {
      url => if(url.trim == "") routes.AccountController.show().url else url
    }

    Redirect(Call("GET", redirectTo))
      .withCookies(ServerCookies.createAuthCookie(userId, enc = true))
      .discardingCookies(DiscardingCookie("att"))
  }

  def logout(): Action[AnyContent] = Action { _ =>
    Redirect(routes.LoginController.loginShow())
      .discardingCookies(DiscardingCookie("aas"))
      .discardingCookies(DiscardingCookie("att"))
  }

  private def checkMFACookie(block: => Future[Result], continue: String => Future[Result])(implicit req: Request[_]): Future[Result] = {
    req
      .cookies
      .get("att")
      .fold(block)(cookie => continue(cookie.getValue()))
  }

  private def checkCookies(block: => Result, continue: => Result)(implicit req: Request[_]): Result = {
    req
      .cookies
      .get("aas")
      .fold(continue)(_ => block)
  }
}
