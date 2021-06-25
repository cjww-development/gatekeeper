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

import controllers.actions.AuthenticatedAction
import forms.AddressForm.{form => addressForm}
import forms.BirthdayForm.{form => birthdayForm}
import forms.ChangeOfPasswordForm.{form => changeOfPasswordForm, _}
import forms.GenderForm.{form => genderForm}
import forms.NameForm.{form => nameForm}
import forms.TOTPSetupCodeVerificationForm
import orchestrators._
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import views.html.account.security.{MFADisableConfirm, Security, TOTP}
import views.html.account.{Account, AccountDetails}
import views.html.misc.{NotFound => NotFoundView, INS}

import javax.inject.Inject
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
      Ok(Security(userInfo.get.mfaEnabled, userInfo.get.emailVerified, userInfo.get.email, userInfo.get.phoneVerified, userInfo.get.phone))
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
            case _ => InternalServerError(INS())
          },
          codes => mfaOrchestrator.postTOTPSetupCodeVerification(userId, codes.codeOne, codes.codeTwo) map {
            _ => Redirect(routes.AccountController.accountSecurity())
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

  def disableMFA(): Action[AnyContent] = authenticatedUser { _ => userId =>
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

  def accountDetails(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    userOrchestrator.getUserDetails(userId) map {
      _.fold(NotFound(NotFoundView()))(user => Ok(AccountDetails(user, emailInUse = false, changeOfPasswordForm, nameForm.fill(user.name), genderForm.fill(user.gender), birthdayForm.fill(user.birthDate.getOrElse("")), user.address.fold(addressForm)(adr => addressForm.fill(adr)))))
    }
  }

  def updateUserEmail(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    val body = req.body.asFormUrlEncoded.getOrElse(Map())
    val emailAddress = body.getOrElse("email", Seq("")).head

    userOrchestrator.updateEmailAndReVerify(userId, emailAddress) flatMap {
      case EmailInUse => userOrchestrator.getUserDetails(userId) map {
        _.fold(NotFound(NotFoundView()))(user => Ok(AccountDetails(user, emailInUse = true, changeOfPasswordForm, nameForm.fill(user.name), genderForm.fill(user.gender), birthdayForm.fill(user.birthDate.getOrElse("")), user.address.fold(addressForm)(adr => addressForm.fill(adr)))))
      }
      case _ => Future.successful(Redirect(routes.AccountController.accountDetails()))
    }
  }

  def updatePassword(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    changeOfPasswordForm.bindFromRequest().fold(
      err => userOrchestrator.getUserDetails(userId) map {
        _.fold(NotFound(NotFoundView()))(user => Ok(AccountDetails(user, emailInUse = false, err, nameForm.fill(user.name), genderForm.fill(user.gender), birthdayForm.fill(user.birthDate.getOrElse("")), user.address.fold(addressForm)(adr => addressForm.fill(adr)))))
      },
      pwd => userOrchestrator.updatePassword(userId, pwd) flatMap {
        case PasswordMismatch => userOrchestrator.getUserDetails(userId) map {
          _.fold(NotFound(NotFoundView()))(user => Ok(AccountDetails(user, emailInUse = false, changeOfPasswordForm.fill(pwd).renderNewPasswordMismatch, nameForm.fill(user.name), genderForm.fill(user.gender), birthdayForm.fill(user.birthDate.getOrElse("")), user.address.fold(addressForm)(adr => addressForm.fill(adr)))))
        }
        case InvalidOldPassword => userOrchestrator.getUserDetails(userId) map {
          _.fold(NotFound(NotFoundView()))(user => Ok(AccountDetails(user, emailInUse = false, changeOfPasswordForm.fill(pwd).renderInvalidOldPasswordError, nameForm.fill(user.name), genderForm.fill(user.gender), birthdayForm.fill(user.birthDate.getOrElse("")), user.address.fold(addressForm)(adr => addressForm.fill(adr)))))
        }
        case _ => Future.successful(Redirect(routes.AccountController.accountDetails()))
      }
    )
  }

  def updateName(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    nameForm.bindFromRequest().fold(
      err => userOrchestrator.getUserDetails(userId) map {
        _.fold(NotFound(NotFoundView()))(user => Ok(AccountDetails(user, emailInUse = false, changeOfPasswordForm, err, genderForm.fill(user.gender), birthdayForm.fill(user.birthDate.getOrElse("")), user.address.fold(addressForm)(adr => addressForm.fill(adr)))))
      },
      name => userOrchestrator.updateName(userId, name) map {
        _ => Redirect(routes.AccountController.accountDetails())
      }
    )
  }

  def updateGender(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    genderForm.bindFromRequest().fold(
      err => userOrchestrator.getUserDetails(userId) map {
        _.fold(NotFound(NotFoundView()))(user => Ok(AccountDetails(user, emailInUse = false, changeOfPasswordForm, nameForm.fill(user.name), err, birthdayForm.fill(user.birthDate.getOrElse("")), user.address.fold(addressForm)(adr => addressForm.fill(adr)))))
      },
      gen => userOrchestrator.updateGender(userId, gen) map {
        _ => Redirect(routes.AccountController.accountDetails())
      }
    )
  }

  def updateBirthday(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    birthdayForm.bindFromRequest().fold(
      err => userOrchestrator.getUserDetails(userId) map {
        _.fold(NotFound(NotFoundView()))(user => Ok(AccountDetails(user, emailInUse = false, changeOfPasswordForm, nameForm.fill(user.name), genderForm.fill(user.gender), err, user.address.fold(addressForm)(adr => addressForm.fill(adr)))))
      },
      hbd => userOrchestrator.updateBirthday(userId, hbd) map {
        _ => Redirect(routes.AccountController.accountDetails())
      }
    )
  }

  def updateAddress(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    addressForm.bindFromRequest().fold(
      err => userOrchestrator.getUserDetails(userId) map {
        _.fold(NotFound(NotFoundView()))(user => Ok(AccountDetails(user, emailInUse = false, changeOfPasswordForm, nameForm.fill(user.name), genderForm.fill(user.gender), birthdayForm.fill(user.birthDate.getOrElse("")), err)))
      },
      adr => userOrchestrator.updateAddress(userId, adr) map {
        _ => Redirect(routes.AccountController.accountDetails())
      }
    )
  }
}
