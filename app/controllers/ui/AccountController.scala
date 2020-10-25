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
import forms.TOTPSetupCodeVerificationForm
import javax.inject.Inject
import orchestrators.{ClientOrchestrator, MFAOrchestrator, MFATOTPQR, UserOrchestrator}
import org.slf4j.LoggerFactory
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import views.html.account.Account
import views.html.account.security.{Security, TOTP, MFADisableConfirm}

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultAccountController @Inject()(val controllerComponents: ControllerComponents,
                                         val clientOrchestrator: ClientOrchestrator,
                                         val mfaOrchestrator: MFAOrchestrator,
                                         val userOrchestrator: UserOrchestrator) extends AccountController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait AccountController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport with AuthenticatedAction {

  val userOrchestrator: UserOrchestrator
  val clientOrchestrator: ClientOrchestrator
  val mfaOrchestrator: MFAOrchestrator

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  private val logger = LoggerFactory.getLogger(this.getClass)

  def show(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    userOrchestrator.getUserDetails(userId) flatMap {
      case Some(userInfo) => if(userId.startsWith("org-user-")) {
        clientOrchestrator.getRegisteredApps(userId, 1) map { clients =>
          Ok(Account(userInfo, clients.flatten))
        }
      } else {
        Future.successful(Ok(Account(userInfo, Seq())))
      }
      case None => Future.successful(Redirect(routes.LoginController.logout()))
    }
  }

  def accountSecurity(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    userOrchestrator.getUserDetails(userId) map { userInfo =>
      Ok(Security(userInfo.get.mfaEnabled, userInfo.get.emailVerified, userInfo.get.email))
    }
  }

  def totpSetup(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    mfaOrchestrator.isMFAEnabled(userId) flatMap { status =>
      if(!status) {
        mfaOrchestrator.setupTOTPMFA(userId) map {
          case MFATOTPQR(qrDataUri) => Ok(TOTP(TOTPSetupCodeVerificationForm.form, qrDataUri))
          case _ => Redirect(routes.LoginController.logout())
        }
      } else {
        Future.successful(Redirect(routes.AccountController.accountSecurity()))
      }
    }
  }

  def postTotpSetupVerification(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    mfaOrchestrator.isMFAEnabled(userId) flatMap { status =>
      if(!status) {
        TOTPSetupCodeVerificationForm.form.bindFromRequest().fold(
          err => mfaOrchestrator.setupTOTPMFA(userId) map {
            case MFATOTPQR(qrCodeData) => BadRequest(TOTP(err, qrCodeData))
          },
          codes => mfaOrchestrator.postTOTPSetupCodeVerification(userId, codes.codeOne, codes.codeTwo) map {
            resp => Redirect(routes.AccountController.accountSecurity())
          }
        )
      } else {
        Future.successful(Redirect(routes.AccountController.accountSecurity()))
      }
    }
  }

  def disableMFAConfirm(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    mfaOrchestrator.isMFAEnabled(userId) map { enabled =>
      if(enabled) {
        Ok(MFADisableConfirm())
      } else {
        Redirect(routes.AccountController.accountSecurity())
      }
    }
  }

  def disableMFA(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    mfaOrchestrator.isMFAEnabled(userId) flatMap { enabled =>
      if(enabled) {
        mfaOrchestrator.disableMFA(userId) map { _ =>
          Redirect(routes.AccountController.accountSecurity())
        }
      } else {
        Future.successful(Redirect(routes.AccountController.accountSecurity()))
      }
    }
  }
}
