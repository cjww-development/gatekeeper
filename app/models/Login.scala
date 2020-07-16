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

import com.cjwwdev.security.obfuscation.Obfuscators
import com.cjwwdev.security.Implicits.ImplicitObfuscator
import play.api.libs.json.{Json, OFormat}
import utils.StringUtils

case class Login(accountId: String,
                 password: String)

object Login extends Obfuscators {
  override val locale: String = "models.Login"

  def apply(accountId: String, salt: String, password: String): Login = new Login(accountId, StringUtils.hasher(salt, password))
  def apply(accountId: String, password: String): Login = new Login(accountId.encrypt, password)
  
  implicit val format: OFormat[Login] = Json.format[Login]
}
