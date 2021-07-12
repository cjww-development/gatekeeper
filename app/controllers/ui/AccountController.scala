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
import views.html.misc.INS

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

  def show(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    if(user.id.startsWith("org-user-")) {
      clientOrchestrator.getRegisteredApps(user.id, 1) map { clients =>
        Ok(Account(user, clients.flatten))
      }
    } else {
      Future.successful(Ok(Account(user, Seq())))
    }
  }

  def accountSecurity(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    Future.successful(Ok(Security(
      user.mfaEnabled,
      user.emailVerified,
      user.email,
      user.phoneVerified,
      user.phone
    )))
  }

  def totpSetup(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    if(!user.mfaEnabled) {
      mfaOrchestrator.setupTOTPMFA(user.id) map {
        case MFATOTPQR(qrDataUri) => Ok(TOTP(TOTPSetupCodeVerificationForm.form, qrDataUri))
      }
    } else {
      Future.successful(Redirect(routes.AccountController.accountSecurity()))
    }
  }

  def postTotpSetupVerification(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    if(!user.mfaEnabled) {
      TOTPSetupCodeVerificationForm.form.bindFromRequest().fold(
        err => mfaOrchestrator.setupTOTPMFA(user.id) map {
          case MFATOTPQR(qrCodeData) => BadRequest(TOTP(err, qrCodeData))
          case _ => InternalServerError(INS())
        },
        codes => mfaOrchestrator.postTOTPSetupCodeVerification(user.id, codes.codeOne, codes.codeTwo) map {
          _ => Redirect(routes.AccountController.accountSecurity())
        }
      )
    } else {
      Future.successful(Redirect(routes.AccountController.accountSecurity()))
    }
  }

  def disableMFAConfirm(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    if(user.mfaEnabled) {
      Future.successful(Ok(MFADisableConfirm()))
    } else {
      Future.successful(Redirect(routes.AccountController.accountSecurity()))
    }
  }

  def disableMFA(): Action[AnyContent] = authenticatedUser { _ => user =>
    if(user.mfaEnabled) {
      mfaOrchestrator.disableMFA(user.id) map { _ =>
        Redirect(routes.AccountController.accountSecurity())
      }
    } else {
      Future.successful(Redirect(routes.AccountController.accountSecurity()))
    }
  }

  def accountDetails(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    Future.successful(Ok(AccountDetails(
      user,
      emailInUse = false,
      changeOfPasswordForm,
      nameForm.fill(user.name),
      genderForm.fill(user.gender),
      birthdayForm.fill(user.birthDate.getOrElse("")),
      user.address.fold(addressForm)(adr => addressForm.fill(adr))
    )))
  }

  def updateUserEmail(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    val body = req.body.asFormUrlEncoded.getOrElse(Map())
    val emailAddress = body.getOrElse("email", Seq("")).head

    userOrchestrator.updateEmailAndReVerify(user.id, emailAddress) map {
      case EmailInUse => BadRequest(AccountDetails(
        user,
        emailInUse = true,
        changeOfPasswordForm,
        nameForm.fill(user.name),
        genderForm.fill(user.gender),
        user.birthDate.fold(birthdayForm)(birthdayForm.fill),
        user.address.fold(addressForm)(addressForm.fill)
      ))
      case _ => Redirect(routes.AccountController.accountDetails())
    }
  }

  def updatePassword(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    changeOfPasswordForm.bindFromRequest().fold(
      err => Future.successful(BadRequest(AccountDetails(
        user,
        emailInUse = false,
        err,
        nameForm.fill(user.name),
        genderForm.fill(user.gender),
        user.birthDate.fold(birthdayForm)(birthdayForm.fill),
        user.address.fold(addressForm)(addressForm.fill)
      ))),
      pwd => userOrchestrator.updatePassword(user.id, pwd) map {
        case PasswordMismatch => BadRequest(AccountDetails(
          user,
          emailInUse = false,
          changeOfPasswordForm.fill(pwd).renderNewPasswordMismatch,
          nameForm.fill(user.name),
          genderForm.fill(user.gender),
          user.birthDate.fold(birthdayForm)(birthdayForm.fill),
          user.address.fold(addressForm)(addressForm.fill)
        ))
        case InvalidOldPassword => BadRequest(AccountDetails(
          user,
          emailInUse = false,
          changeOfPasswordForm.fill(pwd).renderInvalidOldPasswordError,
          nameForm.fill(user.name),
          genderForm.fill(user.gender),
          user.birthDate.fold(birthdayForm)(birthdayForm.fill),
          user.address.fold(addressForm)(addressForm.fill)
        ))
        case _ => Redirect(routes.AccountController.accountDetails())
      }
    )
  }

  def updateName(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    nameForm.bindFromRequest().fold(
      err => Future.successful(BadRequest(AccountDetails(
        user,
        emailInUse = false,
        changeOfPasswordForm,
        err,
        genderForm.fill(user.gender),
        user.birthDate.fold(birthdayForm)(birthdayForm.fill),
        user.address.fold(addressForm)(addressForm.fill)
      ))),
      name => userOrchestrator.updateName(user.id, name) map {
        _ => Redirect(routes.AccountController.accountDetails())
      }
    )
  }

  def updateGender(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    genderForm.bindFromRequest().fold(
      err => Future.successful(BadRequest(AccountDetails(
        user,
        emailInUse = false,
        changeOfPasswordForm,
        nameForm.fill(user.name),
        err,
        user.birthDate.fold(birthdayForm)(birthdayForm.fill),
        user.address.fold(addressForm)(addressForm.fill)
      ))),
      gen => userOrchestrator.updateGender(user.id, gen) map {
        _ => Redirect(routes.AccountController.accountDetails())
      }
    )
  }

  def updateBirthday(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    birthdayForm.bindFromRequest().fold(
      err => Future.successful(BadRequest(AccountDetails(
        user,
        emailInUse = false,
        changeOfPasswordForm,
        nameForm.fill(user.name),
        genderForm.fill(user.gender),
        err,
        user.address.fold(addressForm)(addressForm.fill)
      ))),
      hbd => userOrchestrator.updateBirthday(user.id, hbd) map {
        _ => Redirect(routes.AccountController.accountDetails())
      }
    )
  }

  def updateAddress(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    addressForm.bindFromRequest().fold(
      err => Future.successful(BadRequest(AccountDetails(
        user,
        emailInUse = false,
        changeOfPasswordForm,
        nameForm.fill(user.name),
        genderForm.fill(user.gender),
        user.birthDate.fold(birthdayForm)(birthdayForm.fill),
        err
      ))),
      adr => userOrchestrator.updateAddress(user.id, adr) map {
        _ => Redirect(routes.AccountController.accountDetails())
      }
    )
  }
}
