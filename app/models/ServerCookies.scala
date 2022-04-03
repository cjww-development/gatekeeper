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

import dev.cjww.security.Implicits.ImplicitObfuscator
import dev.cjww.security.deobfuscation.DeObfuscators
import dev.cjww.security.obfuscation.Obfuscators
import play.api.mvc.Cookie

object ServerCookies extends Obfuscators with DeObfuscators {
  override val locale: String = "models.ServerCookies"

  def createAuthCookie(contents: String, enc: Boolean): Cookie = {
    Cookie(name = "aas", value = if(enc) contents.encrypt else contents)
  }

  def createMFAChallengeCookie(contents: String, enc: Boolean): Cookie = {
    Cookie(name = "att", value = if(enc) contents.encrypt else contents)
  }

  implicit class CookieOps(cookie: Cookie) {
    def getValue(): String = {
      stringDeObfuscate.decrypt(cookie.value).fold(
        _ => cookie.value,
        identity
      )
    }
  }
}
