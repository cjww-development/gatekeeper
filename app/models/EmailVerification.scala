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

import com.cjwwdev.security.obfuscation.{Obfuscation, Obfuscator}
import org.bson.codecs.configuration.CodecProvider
import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.Macros
import play.api.libs.json.{Json, OFormat}

case class EmailVerification(verificationId: String,
                             userId: String,
                             email: String,
                             accType: String,
                             createdAt: DateTime)

object EmailVerification extends TimeFormat with Obfuscation {
  override val locale: String = ""
  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[EmailVerification]()
  implicit val format: OFormat[EmailVerification] = Json.format[EmailVerification]
  implicit val obfuscator: Obfuscator[EmailVerification] = (value: EmailVerification) => obfuscate(Json.toJson(value))
}
