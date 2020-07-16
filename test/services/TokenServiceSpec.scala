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

package services

import helpers.Assertions
import org.apache.commons.net.util.Base64
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class TokenServiceSpec
  extends PlaySpec
    with Assertions {

  private val testService: TokenService = new TokenService {
    override val issuer: String    = "testIssuer"
    override val expiry: Long      = 30000
    override val signature: String = "testSignature"
  }

  "createAccessToken" should {
    "return a signed access token" when {
      "given an owner and account type" in {
        assertOutput(testService.createAccessToken("testOwner", "testAccType")) { token =>
          println(token)
          val split = token.split("\\.")
          split.length mustBe 3

          Base64.decodeBase64(split(0)).map(_.toChar).mkString mustBe """{"typ":"JWT","alg":"HS512"}"""
          val payload = Json.parse(Base64.decodeBase64(split(1)).map(_.toChar).mkString)
          payload.\("iss").as[String] mustBe "testIssuer"
          payload.\("sub").as[String] mustBe "testOwner"
          payload.\("accType").as[String] mustBe "testAccType"
        }
      }
    }
  }
}
