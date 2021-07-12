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

package api

import database.CodecReg
import helpers.{Assertions, IntegrationApp}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class JwksGetApisISpec
  extends PlaySpec
    with IntegrationApp
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CodecReg
    with Assertions {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  override def afterEach(): Unit = {
    super.afterEach()
  }

  "GET /api/oauth2/jwks" should {
    "return an Ok" when {
      "a set of JWKs has been returned" in {
        val result = ws
          .url(s"$testAppUrl/api/oauth2/jwks")
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          resp.json.\("keys").as[List[JsValue]].length mustBe 1
        }
      }
    }
  }
}
