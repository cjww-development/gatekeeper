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

import errors.StandardErrors
import models.Verification._
import orchestrators._
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import views.html.misc.{INS, NotFound => NotFoundView}
import views.html.registration.{EmailVerified => EmailVerifiedView}

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultEmailController @Inject()(val controllerComponents: ControllerComponents,
                                       val registrationOrchestrator: RegistrationOrchestrator) extends EmailController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait EmailController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport {

  implicit val ec: ExC

  val registrationOrchestrator: RegistrationOrchestrator

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  def verifyUserEmailAddress(payload: String): Action[AnyContent] = Action.async { implicit req =>
    deObfuscator.decrypt(payload).fold(
      _ => Future.successful(BadRequest(StandardErrors.INVALID_REQUEST)),
      rec => registrationOrchestrator.confirmEmailAddress(rec) map {
        case EmailVerified => Ok(EmailVerifiedView())
        case NoRecordFound => NotFound(NotFoundView())
        case ErrorRetryAllowed => InternalServerError(INS())
      }
    )
  }
}
