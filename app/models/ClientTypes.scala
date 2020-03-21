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

package models

import play.api.libs.json.{JsResult, JsSuccess, JsValue, Reads}

object ClientTypes extends Enumeration {

  val confidential: ClientTypes.Value = Value
  val public: ClientTypes.Value = Value

  implicit val reads = new Reads[ClientTypes.Value] {
    override def reads(json: JsValue): JsResult[ClientTypes.Value] = {
      json.as[String] match {
        case "confidential" => JsSuccess(confidential)
        case "public"       => JsSuccess(public)
      }
    }
  }
}
