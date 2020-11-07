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
import play.api.libs.json.{Json, OFormat}

case class UserInfo(id: String,
                    userName: String,
                    email: String,
                    emailVerified: Boolean,
                    accType: String,
                    name: Name,
                    gender: Gender,
                    authorisedClients: List[AuthorisedClient],
                    mfaEnabled: Boolean,
                    createdAt: DateTime) {

  val toMap: Map[String, String] = Map(
   "id" -> id,
   "username" -> userName,
   "email" -> email,
   "act" -> accType
  )
}

object UserInfo extends TimeFormat {
  implicit val format: OFormat[UserInfo] = Json.format[UserInfo]
}


