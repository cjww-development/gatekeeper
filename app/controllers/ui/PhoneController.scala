/*
 * Copyright 2022 CJWW Development
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
import forms.CodeForm.{form => codeForm}
import forms.PhoneForm.{form => phoneForm}
import orchestrators._
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import views.html.account.security.phone.{EnterCode, EnterNumber}
import views.html.misc.{INS, NotFound => NotFoundView}

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultPhoneController @Inject()(val controllerComponents: ControllerComponents,
                                       val userOrchestrator: UserOrchestrator,
                                       val registrationOrchestrator: RegistrationOrchestrator) extends PhoneController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait PhoneController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport with AuthenticatedAction {

  implicit val ec: ExC

  val registrationOrchestrator: RegistrationOrchestrator

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  def enterPhoneNumber(): Action[AnyContent] = authenticatedUser { implicit req => _ =>
    Future.successful(Ok(EnterNumber(phoneForm)))
  }

  def submitPhoneNumber(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    phoneForm.bindFromRequest().fold(
      err => Future.successful(BadRequest(EnterNumber(err))),
      num => registrationOrchestrator.sendPhoneVerificationMessage(user.id, num) map {
        case VerificationSent => Redirect(routes.PhoneController.enterVerifyCode())
        case NoUserFound => NotFound(NotFoundView())
        case _  => InternalServerError(INS())
      }
    )
  }

  def enterVerifyCode(): Action[AnyContent] = authenticatedUser { implicit req => _ =>
    Future.successful(Ok(EnterCode(codeForm)))
  }

  def verifyEnteredCode(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    codeForm.bindFromRequest().fold(
      err  => Future.successful(BadRequest(EnterCode(err))),
      code => registrationOrchestrator.verifySentCode(user.id, code) map {
        case PhoneVerified => Redirect(routes.AccountController.accountSecurity())
        case _             => NotFound(NotFoundView())
      }
    )
  }
}
