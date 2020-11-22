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

package controllers.api

import helpers.Assertions
import helpers.orchestrators.MockWellKnownConfigOrchestrator
import models.WellKnownConfig
import orchestrators.WellKnownConfigOrchestrator
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ConfigControllerSpec
  extends PlaySpec
    with Assertions
    with MockWellKnownConfigOrchestrator {

  val testController: ConfigController = new ConfigController {
    override val wellKnownConfigOrchestrator: WellKnownConfigOrchestrator = mockWellKnownConfigOrchestrator
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  val testWkc: WellKnownConfig = WellKnownConfig(
    issuer = "testIssuer",
    authorizationEndpoint = "testAuthEndpoint",
    tokenEndpoint = "testTokenEndpoint",
    userInfoEndpoint = "testUserInfoEndpoint",
    jwksUri = "testJwksEndpoint",
    registrationEndpoint = "",
    scopesSupported = Seq("testScope"),
    responseTypesSupported = Seq("testResponseType"),
    grantTypesSupported = Seq("testGrantType"),
    tokenEndpointAuth = Seq("testAuth"),
    revokeEndpoint = "testRevokeEndpoint",
    idTokenSigningAlgs = Seq("testAlg")
  )

  "wellKnownConfig" should {
    "return an Ok" when {
      "getting the servers well known config" in {
        mockGetWellKnownConfig(wkc = testWkc)

        assertFutureResult(testController.wellKnownConfig()(FakeRequest())) { res =>
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.parse(
            """
              |{
              | "issuer":"testIssuer",
              | "authorization_endpoint":"testAuthEndpoint",
              | "token_endpoint":"testTokenEndpoint",
              | "grant_types_supported":["testGrantType"],
              | "scopes_supported":["testScope"],
              | "response_types_supported":["testResponseType"],
              | "token_endpoint_auth_methods_supported":["testAuth"],
              | "revocation_endpoint":"testRevokeEndpoint",
              | "revocation_endpoint_auth_methods_supported":["testAuth"],
              | "id_token_signing_alg_values_supported":["testAlg"],
              | "token_endpoint_auth_methods_supported":["testAuth"],
              | "jwks_endpoint":"testJwksEndpoint",
              | "userinfo_endpoint":"testUserInfoEndpoint"
              |}
            """.stripMargin
          )
        }
      }
    }
  }
}
