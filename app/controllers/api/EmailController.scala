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

package controllers.api

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.i18n.Lang
import play.api.mvc._

import scala.concurrent.{ExecutionContext => ExC}

class DefaultEmailController @Inject()(val controllerComponents: ControllerComponents) extends EmailController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait EmailController extends BaseController {

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  private val logger = LoggerFactory.getLogger(this.getClass)
}
