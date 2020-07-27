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

import com.cjwwdev.security.Implicits.ImplicitObfuscator
import com.cjwwdev.security.deobfuscation.DeObfuscators
import com.cjwwdev.security.obfuscation.Obfuscators
import helpers.Assertions
import models.ServerCookies.CookieOps
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Cookie

class ServerCookiesSpec extends PlaySpec with Assertions with Obfuscators with DeObfuscators {

  override val locale: String = "models.ServerCookies"

  "createAuthCookie" should {
    "return an encrypted play Cookie" in {
      val expected = Cookie(
        name  = "aas",
        value = "value".encrypt
      )

      assertOutput(ServerCookies.createAuthCookie("value", enc = true)) { cookie =>
        cookie.name mustBe expected.name
        cookie.value mustBe expected.value
      }
    }

    "return an unencrypted play Cookie" in {
      val expected = Cookie(
        name  = "aas",
        value = "value"
      )

      assertOutput(ServerCookies.createAuthCookie("value", enc = false)) { cookie =>
        cookie.name mustBe expected.name
        cookie.value mustBe expected.value
      }
    }
  }

  "implicitly getValue" should {
    "decrypt the cookie value if the value is encrypted" in {
      val testInput = Cookie(
        name  = "test",
        value = "value"
      )

      assertOutput(testInput.getValue()) {
        _ mustBe "value"
      }
    }

    "return the raw string if there was a problem decrypting" in {
      val testInput = Cookie(
        name  = "test",
        value = "value"
      )

      assertOutput(testInput.getValue()) {
        _ mustBe "value"
      }
    }
  }
}
