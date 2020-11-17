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
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._
import services.JwksService

import scala.concurrent.{ExecutionContext => ExC}

class DefaultJwksController @Inject()(val controllerComponents: ControllerComponents,
                                      val jwksService: JwksService) extends JwksController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait JwksController extends BaseController {

  implicit val ec: ExC

  val jwksService: JwksService

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getCurrentJwks(): Action[AnyContent] = Action { implicit req =>
    val jwks = jwksService.getCurrentJwks
    Ok(Json.obj("keys" -> JsArray(Seq(Json.parse(jwks.toPublicJWK.toJSONString)))))
  }
}
