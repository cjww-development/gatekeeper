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
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

class ConfigGetApisISpec
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

  "GET /api/.well-known/openid-configuration" should {
    "return an Ok" when {
      "the well known config endpoint returns the relevant config" in {
        val result = ws
          .url(s"$testAppUrl/api/.well-known/openid-configuration")
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          resp.json mustBe Json.parse(
            s"""
              |{
              | "issuer":"http://localhost:$port",
              | "authorization_endpoint":"http://localhost:$port/gatekeeper/oauth2/authorize",
              | "token_endpoint":"http://localhost:$port/gatekeeper/oauth2/token",
              | "userinfo_endpoint":"http://localhost:$port/gatekeeper/api/oauth2/userinfo",
              | "jwks_uri":"http://localhost:$port/gatekeeper/api/oauth2/jwks",
              | "scopes_supported":[
              |   "openid",
              |   "profile",
              |   "email",
              |   "address",
              |   "phone"
              | ],
              | "response_types_supported":[
              |   "code"
              | ],
              | "grant_types_supported":[
              |   "authorization_code",
              |   "client_credentials",
              |   "refresh_token"
              | ],
              | "id_token_signing_alg_values_supported":["RS256"],
              | "token_endpoint_auth_methods_supported":[
              |   "client_secret_basic",
              |   "client_secret_post"
              | ],
              | "revocation_endpoint":"http://localhost:$port/gatekeeper/api/oauth2/revoke",
              | "revocation_endpoint_auth_methods_supported":[
              |   "client_secret_basic",
              |   "client_secret_post"
              | ]
              |}
            """.stripMargin
          )
        }
      }
    }
  }
}
