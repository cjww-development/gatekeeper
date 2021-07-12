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

package controllers.api

import models.WellKnownConfig
import orchestrators.WellKnownConfigOrchestrator
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.Inject

class DefaultConfigController @Inject()(val controllerComponents: ControllerComponents,
                                        val wellKnownConfigOrchestrator: WellKnownConfigOrchestrator) extends ConfigController

trait ConfigController extends BaseController {

  val wellKnownConfigOrchestrator: WellKnownConfigOrchestrator

  def wellKnownConfig(): Action[AnyContent] = Action { implicit req =>
    val config = wellKnownConfigOrchestrator.getConfig
    Ok(Json.toJson(config)(WellKnownConfig.writer))
  }
}
