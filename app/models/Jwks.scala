/*
 * Copyright 2021 CJWW Development
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

case class Jwks(kty: String,
                e: String,
                use: String,
                kid: String,
                alg: String,
                n: String)

object Jwks extends Obfuscation with DeObfuscation {
  override val locale: String = ""

  implicit val format: OFormat[Jwks] = Json.format[Jwks]

  private val obs: Obfuscator[Jwks] = (value: Jwks) => obfuscate(Json.toJson(value))
  private val deObs: DeObfuscator[Jwks] = (value: String) => deObfuscate(value)

  def encrypt(jwks: Jwks): String = obs.encrypt(jwks)
  def decrypt(jwks: String): Either[DecryptionError, Jwks] = deObs.decrypt(jwks)
}
