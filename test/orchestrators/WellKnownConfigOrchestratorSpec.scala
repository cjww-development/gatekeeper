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
import play.api.mvc.request.RemoteConnection
import play.api.mvc.{AnyContentAsEmpty, RequestHeader}
import play.api.test.FakeRequest

import java.net.InetAddress
import java.security.cert.X509Certificate

class WellKnownConfigOrchestratorSpec
  extends PlaySpec
    with Assertions {

  val fakeInsecureRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "http://localhost:5678")
  val fakeSecureRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "https://localhost:5678")
    .withConnection(new RemoteConnection {
      override def remoteAddress: InetAddress = ???
      override def secure: Boolean = true
      override def clientCertificateChain: Option[Seq[X509Certificate]] = ???
    })

  val testOrchestrator: WellKnownConfigOrchestrator = new WellKnownConfigOrchestrator {
    override val authEndpoint: RequestHeader => String = rh => "testAuthEndpoint"
    override val tokenEndpoint: RequestHeader => String = rh => "testTokenEndpoint"
    override val grantTypes: Seq[String] = Seq("testGrantType")
    override val supportedScopes: Seq[String] = Seq("testScope")
    override val responseTypes: Seq[String] = Seq("testResponseType")
    override val revokeEndpoint: RequestHeader => String = rh => "testRevokeEndpoint"
    override val tokenEndpointAuth: Seq[String] = Seq("testAuth")
    override val userDetailsEndpoint: RequestHeader => String = rh => "testUserInfoEndpoint"
    override val jwksEndpoint: RequestHeader => String = rh => "testJwksEndpoint"
    override val idTokenAlgs: Seq[String] = Seq("testAlg")
  }

  val testInsecureWkc: WellKnownConfig = WellKnownConfig(
    "http://localhost:5678",
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

  val testSecureWkc: WellKnownConfig = WellKnownConfig(
    "https://localhost:5678",
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

  "getConfig" should {
    "return an insecure WellKnownConfig" in {
      assertOutput(testOrchestrator.getConfig(fakeInsecureRequest)) {
        _ mustBe testInsecureWkc
      }
    }

    "return an secure WellKnownConfig" in {
      assertOutput(testOrchestrator.getConfig(fakeSecureRequest)) {
        _ mustBe testSecureWkc
      }
    }
  }
}
