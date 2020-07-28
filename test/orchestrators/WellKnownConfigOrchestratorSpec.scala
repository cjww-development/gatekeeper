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

package orchestrators

import helpers.Assertions
import models.WellKnownConfig
import org.scalatestplus.play.PlaySpec

class WellKnownConfigOrchestratorSpec
  extends PlaySpec
    with Assertions {

  val testOrchestrator: WellKnownConfigOrchestrator = new WellKnownConfigOrchestrator {
    override val issuer: String = "testIssuer"
    override val authEndpoint: String = "testAuthEndpoint"
    override val tokenEndpoint: String = "testTokenEndpoint"
    override val grantTypes: Seq[String] = Seq("testGrantType")
  }

  val testWkc = WellKnownConfig(
    "testIssuer",
    authorizationEndpoint = "testAuthEndpoint",
    tokenEndpoint = "testTokenEndpoint",
    userInfoEndpoint = "",
    jwksUri = "",
    registrationEndpoint = "",
    scopesSupported = Seq(),
    responseTypesSupported = Seq(),
    grantTypesSupported = Seq("testGrantType"),
    subjectTypesSupported = Seq(),
    idTokenSigningAlgValuesSupported = Seq()
  )

  "getConfig" should {
    "return a WellKnownConfig" in {
      assertOutput(testOrchestrator.getConfig) {
        _ mustBe testWkc
      }
    }
  }
}
