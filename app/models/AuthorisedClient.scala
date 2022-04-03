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

package models

import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.Macros
import play.api.libs.json.{Json, OFormat}

import scala.reflect.ClassTag

case class AuthorisedClient(appId: String,
                            authorisedScopes: Seq[String],
                            authorisedOn: DateTime)

object AuthorisedClient extends TimeFormat {
  val codec = Macros.createCodecProviderIgnoreNone[AuthorisedClient]()

  implicit val classTag: ClassTag[AuthorisedClient] = ClassTag[AuthorisedClient](classOf[AuthorisedClient])

  implicit val scopeFormat: OFormat[Scope] = Json.format[Scope]
  implicit val format: OFormat[AuthorisedClient] = Json.format[AuthorisedClient]
}
