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
import forms.RegistrationForm.{form => regForm, _}
import javax.inject.Inject
import orchestrators._
import org.slf4j.LoggerFactory
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import views.html.registration.{UserRegistration, RegSuccess}

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultRegistrationController @Inject()(val controllerComponents: ControllerComponents,
                                              val userOrchestrator: UserOrchestrator,
                                              val registrationOrchestrator: RegistrationOrchestrator) extends RegistrationController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait RegistrationController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport with AuthenticatedFilter {

  val registrationOrchestrator: RegistrationOrchestrator

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  private val logger = LoggerFactory.getLogger(this.getClass)

  def showUserReg(): Action[AnyContent] = Action { implicit req =>
    Ok(UserRegistration(regForm))
  }

  def submitUserReg(): Action[AnyContent] = Action.async { implicit req =>
    regForm.bindFromRequest.fold(
      errs => Future.successful(BadRequest(errs.toString)),
      user => registrationOrchestrator.registerUser(user) map {
        case Registered        => Ok(RegSuccess())
        case RegistrationError => InternalServerError
        case _                 => BadRequest(UserRegistration(regForm.renderErrors))
      }
    )
  }
}
