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

import dev.cjww.security.DecryptionError
import dev.cjww.security.deobfuscation.{DeObfuscation, DeObfuscator}
import dev.cjww.security.obfuscation.{Obfuscation, Obfuscator}
import play.api.libs.json.{Json, OFormat}

case class RefreshToken(sub: String,
                        aud: String,
                        iss: String,
                        iat: Long,
                        exp: Long,
                        tsid: String,
                        tid: String,
                        scope: Seq[String])

object RefreshToken extends Obfuscation with DeObfuscation {
  override val locale: String = ""

  implicit val format: OFormat[RefreshToken] = Json.format[RefreshToken]

  implicit val obs: Obfuscator[RefreshToken] = (value: RefreshToken) => obfuscate(Json.toJson(value))

  implicit val deObs: DeObfuscator[RefreshToken] = (value: String) => deObfuscate(value)

  def enc(token: RefreshToken): String = obs.encrypt(token)
  def dec(token: String): Either[DecryptionError, RefreshToken] = deObs.decrypt(token)
}
