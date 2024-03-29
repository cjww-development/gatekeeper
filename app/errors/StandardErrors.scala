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

package errors

import play.api.libs.json.{JsObject, Json}

object StandardErrors {
  val INVALID_REQUEST: JsObject = Json.obj("error" -> "invalid_request")
  val INVALID_CLIENT: JsObject = Json.obj("error" -> "invalid_client")
  val INVALID_GRANT: JsObject = Json.obj("error" -> "invalid_grant")
  val UNAUTHORIZED_CLIENT: JsObject = Json.obj("error" -> "unauthorized_client")
  val UNSUPPORTED_TOKEN_TYPE: JsObject = Json.obj("error" -> "unsupported_token_type")
  val INVALID_TOKEN: JsObject = Json.obj("error" -> "invalid_token")
}
