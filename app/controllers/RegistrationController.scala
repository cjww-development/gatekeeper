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

package controllers

import forms.RegistrationForm
import javax.inject.Inject
import orchestrators.{BothInUse, EmailInUse, Registered, RegistrationError, RegistrationOrchestrator, UserNameInUse}
import org.slf4j.LoggerFactory
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, RequestHeader}
import views.html.registration.UserRegistration

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultRegistrationController @Inject()(val controllerComponents: ControllerComponents,
                                              val registrationOrchestrator: RegistrationOrchestrator) extends RegistrationController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait RegistrationController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport {

  val registrationOrchestrator: RegistrationOrchestrator

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader) = messagesApi.preferred(rh).lang

  private val logger = LoggerFactory.getLogger(this.getClass)

  def show(): Action[AnyContent] = Action { implicit req =>
    Ok(UserRegistration(RegistrationForm.form))
  }

  def submit(): Action[AnyContent] = Action.async { implicit req =>
    RegistrationForm.form.bindFromRequest().fold(
      errs => Future.successful(BadRequest(errs.toString)),
      user => registrationOrchestrator.registerUser(user) map {
        case Registered        => Ok(user.toString)
        case BothInUse         => BadRequest("Email and user name are in use")
        case EmailInUse        => BadRequest("Email is in use")
        case UserNameInUse     => BadRequest("User name is in use")
        case RegistrationError => BadRequest("There was a problem registering the new user")
      }
    )
  }
}
