/*
 * Copyright 2022 CJWW Development
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

package controllers.shuttering

import dev.cjww.http.responses.ApiResponse
import dev.cjww.shuttering.controllers.ShutteringController
import play.api.libs.json.JsValue
import play.api.mvc._

import javax.inject.Inject

class DefaultShutteringController @Inject()(val controllerComponents: ControllerComponents)
  extends ShutteringController with ApiResponse {

  override def validateAdminCall(f: RequestHeader => Result): Action[AnyContent] = Action { req =>
    //TODO: Decide on the admin call validation
    f(req)
  }

  override def jsonResponse(status: Int, body: JsValue)(f: JsValue => Result)(implicit rh: RequestHeader): Result = {
    withJsonResponseBody(status, body) {
      json => Status(status)(json)
    }
  }
}
