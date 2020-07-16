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

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

case class Grant(authCode: String,
                 scope: Seq[String],
                 userId: String,
                 accType: String,
                 createdAt: DateTime)

object Grant {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  implicit val timeFormat = new Format[DateTime] {
    override def writes(o: DateTime): JsValue = JsString(o.toString())

    override def reads(json: JsValue): JsResult[DateTime] = {
      json.validate[String].map[DateTime](dtString =>
        DateTime.parse(dtString, DateTimeFormat.forPattern(dateFormat))
      )
    }
  }

  implicit val format: OFormat[Grant] = Json.format[Grant]

  val outboundWriter: Writes[Grant] = (o: Grant) => Json.obj(
    "authCode" -> o.authCode
  )
}
