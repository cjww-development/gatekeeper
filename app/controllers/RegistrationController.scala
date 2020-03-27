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

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import views.html.registration.UserRegistration

class DefaultRegistrationController @Inject()(val controllerComponents: ControllerComponents) extends RegistrationController

trait RegistrationController extends BaseController {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def show(): Action[AnyContent] = Action { implicit req =>
    Ok(UserRegistration())
  }

  def submit(): Action[AnyContent] = Action { req =>
    Ok(req.body.toString)
  }
}
