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

package controllers.actions

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.jwk.{KeyUse, RSAKey}
import database.TokenRecordStore
import helpers.Assertions
import helpers.database.MockTokenRecordStore
import helpers.services.MockTokenService
import models.TokenRecord
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.mvc.{BaseController, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.TokenService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OAuthActionSpec
  extends PlaySpec
    with MockTokenService
    with MockTokenRecordStore
    with Assertions {

  private val testFilter = new OAuthAction with BaseController {
    override val signature: String = "test-signing-key"
    override val tokenService: TokenService = mockTokenService
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  val rsaKeyGenerator: RSAKeyGenerator = new RSAKeyGenerator(2048)
  val rsaKey = rsaKeyGenerator
    .keyUse(KeyUse.SIGNATURE)
    .algorithm(new Algorithm("RS256"))
    .keyID("testKeyId")
    .generate()

  private val testTokenService = new TokenService {
    override val jwks: RSAKey = rsaKey
    override val tokenRecordStore: TokenRecordStore = mockTokenRecordStore
    override val issuer: String = "testIssuer"
    override val expiry: Long = 900000L
    override val signature: String = "test-signing-key"
  }

  private val okFunction: (String, Seq[String]) => Future[Result] = (userId, scopes) => Future.successful(
    Ok(s"UserId: $userId with scopes ${scopes.mkString}")
  )

  "authorised" should {
    "return an Ok" when {
      "the token has been deemed valid and the userId and scope has been returned" in {
        val accessToken = testTokenService.createAccessToken(
          "testClientId",
          "testUserId",
          "testSetId",
          "testTokenId",
          "openid profile",
          testTokenService.expiry
        )

        val req = FakeRequest()
          .withHeaders(("Authorization", s"Bearer $accessToken"))

        mockLookupTokenRecordSet(recordSet = Some(TokenRecord(
          tokenSetId = "testSetId",
          userId = "testUserId",
          appId = "testClientId",
          accessTokenId = "testTokenId",
          idTokenId = None,
          refreshTokenId = None,
          issuedAt = new DateTime()
        )))

        val res = testFilter.authorised {
          _ => userId => scopes => okFunction(userId, scopes)
        }.apply(req)

        assertOutput(res) { res =>
          status(res)          mustBe OK
          contentAsString(res) mustBe "UserId: testUserId with scopes openid profile"
        }
      }
    }

    "return an invalid_token response" when {
      "the token was valid the token record was found but the access token Id didn't match" in {
        val accessToken = testTokenService.createAccessToken(
          "testClientId",
          "testUserId",
          "testSetId",
          "testTokenId",
          "openid profile",
          testTokenService.expiry
        )

        val req = FakeRequest()
          .withHeaders(("Authorization", s"Bearer $accessToken"))

        mockLookupTokenRecordSet(recordSet = Some(TokenRecord(
          tokenSetId = "testSetId",
          userId = "testUserId",
          appId = "testClientId",
          accessTokenId = "testTokenId2",
          idTokenId = None,
          refreshTokenId = None,
          issuedAt = new DateTime()
        )))

        val res = testFilter.authorised {
          _ => userId => scopes => okFunction(userId, scopes)
        }.apply(req)

        assertOutput(res) { res =>
          status(res)        mustBe UNAUTHORIZED
          contentAsJson(res) mustBe Json.obj("error" -> "invalid_token")
        }
      }

      "the token was valid but the tsid wasn't found " in {
        val accessToken = testTokenService.createAccessToken(
          "testClientId",
          "testUserId",
          "testSetId",
          "testTokenId",
          "openid profile",
          testTokenService.expiry
        )

        val req = FakeRequest()
          .withHeaders(("Authorization", s"Bearer $accessToken"))

        mockLookupTokenRecordSet(recordSet = None)

        val res = testFilter.authorised {
          _ => userId => scopes => okFunction(userId, scopes)
        }.apply(req)

        assertOutput(res) { res =>
          status(res)        mustBe UNAUTHORIZED
          contentAsJson(res) mustBe Json.obj("error" -> "invalid_token")
        }
      }

      "the auth header wasn't in the correct format" in {
        val req = FakeRequest()
          .withHeaders(("Authorization", s"Basic user:pass"))

        val res = testFilter.authorised {
          _ => userId => scopes => okFunction(userId, scopes)
        }.apply(req)

        assertOutput(res) { res =>
          status(res)        mustBe UNAUTHORIZED
          contentAsJson(res) mustBe Json.obj("error" -> "invalid_token")
        }
      }

      "the auth header doesn't exist" in {
        val req = FakeRequest()

        val res = testFilter.authorised {
          _ => userId => scopes => okFunction(userId, scopes)
        }.apply(req)

        assertOutput(res) { res =>
          status(res)        mustBe UNAUTHORIZED
          contentAsJson(res) mustBe Json.obj("error" -> "invalid_token")
        }
      }
    }
  }
}
