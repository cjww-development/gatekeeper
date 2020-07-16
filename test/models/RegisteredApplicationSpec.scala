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

import helpers.Assertions
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, Json}

class RegisteredApplicationSpec extends PlaySpec with Assertions {

  "A registered app Json value" should {
    "be transformed into a valid case class" when {
      "the client type is confidential" in {
        val testJson = Json.obj(
          "owner"       -> "testOwner",
          "name"        -> "test-confidential-app",
          "desc"        -> "test app",
          "homeUrl"     -> "http://localhost:5678",
          "redirectUrl" -> "http://localhost:5678/redirect",
          "clientType"  -> "confidential"
        )

        val result = Json.fromJson[RegisteredApplication](testJson).get

        assertOutput(result) { res =>
          res.name        mustBe "test-confidential-app"
          res.desc        mustBe "test app"
          res.homeUrl     mustBe "http://localhost:5678"
          res.redirectUrl mustBe "http://localhost:5678/redirect"
          res.clientType  mustBe "confidential"

          assert(res.clientType != "")
          assert(res.clientSecret.nonEmpty)
        }
      }

      "the client type is public" in {
        val testJson = Json.obj(
          "owner"       -> "testOwner",
          "name"        -> "test-public-app",
          "desc"        -> "test app",
          "homeUrl"     -> "http://localhost:5678",
          "redirectUrl" -> "http://localhost:5678/redirect",
          "clientType"  -> "public"
        )

        val result = Json.fromJson[RegisteredApplication](testJson).get

        assertOutput(result) { res =>
          res.name        mustBe "test-public-app"
          res.desc        mustBe "test app"
          res.homeUrl     mustBe "http://localhost:5678"
          res.redirectUrl mustBe "http://localhost:5678/redirect"
          res.clientType  mustBe "public"

          assert(res.clientType != "")
          assert(res.clientSecret.isEmpty)
        }
      }
    }

    "return a JsError" when {
      "the client type is unknown" in {
        val testJson = Json.obj(
          "owner"       -> "testOwner",
          "name"        -> "test",
          "desc"        -> "test app",
          "homeUrl"     -> "http://localhost:5678",
          "redirectUrl" -> "http://localhost:5678/redirect",
          "clientType"  -> "invalid"
        )

        val result = Json.fromJson[RegisteredApplication](testJson)

        assertOutput(result) { res =>
          assert(res.isError)

          res mustBe JsError(JsPath.\("clientType"), "Unsupported client type")
        }
      }
    }
  }
}
