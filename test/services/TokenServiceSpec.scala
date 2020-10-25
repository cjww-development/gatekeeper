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

import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoSuccessCreate}
import database.TokenRecordStore
import helpers.Assertions
import helpers.database.MockTokenRecordStore
import models.{AuthorisedClient, TokenRecord, UserInfo}
import org.apache.commons.net.util.Base64
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global

class TokenServiceSpec
  extends PlaySpec
    with Assertions
    with MockTokenRecordStore {

  private val testService: TokenService = new TokenService {
    override val issuer: String    = "testIssuer"
    override val expiry: Long      = 30000
    override val signature: String = "testSignature"
    override val tokenRecordStore: TokenRecordStore = mockTokenRecordStore
  }

  val now: DateTime = DateTime.now()

  val userInfo = UserInfo(
    id = "test-user-id",
    userName = "test-org",
    email = "test@email.com",
    emailVerified = true,
    accType = "organisation",
    authorisedClients = List.empty[AuthorisedClient],
    mfaEnabled = false,
    createdAt = now
  )

  val tokenRecordSet = TokenRecord(
    tokenSetId = "testTokenSetId",
    userId = "testUserId",
    appId = "testAppId",
    accessTokenId = "testTokenId",
    idTokenId = Some("testTokenId"),
    refreshTokenId = Some("testTokenId"),
    issuedAt = now
  )

  "createAccessToken" should {
    "return a signed access token" when {
      "given a clientId, userId and scope" in {
        assertOutput(testService.createAccessToken("testClientId", "testUserId", "testSetId", "testTokenId", "openid")) { token =>
          val split = token.split("\\.")
          split.length mustBe 3

          Base64.decodeBase64(split(0)).map(_.toChar).mkString mustBe """{"typ":"JWT","alg":"HS512"}"""
          val payload = Json.parse(Base64.decodeBase64(split(1)).map(_.toChar).mkString)
          payload.\("aud").as[String] mustBe "testClientId"
          payload.\("iss").as[String] mustBe "testIssuer"
          payload.\("sub").as[String] mustBe "testUserId"
          payload.\("scp").as[String] mustBe "openid"
          payload.\("tsid").as[String] mustBe "testSetId"
          payload.\("tid").as[String] mustBe "testTokenId"
        }
      }
    }
  }

  "createIdToken" should {
    "return a signed id token" when {
      "given a clientId, userId, user details and account type" in {
        val userDetails = Map(
          "" -> ""
        )

        assertOutput(testService.createIdToken("testClientId", "testUserId", "testSetId", "testTokenId", userInfo.toMap)) { token =>
          val split = token.split("\\.")
          split.length mustBe 3

          Base64.decodeBase64(split(0)).map(_.toChar).mkString mustBe """{"typ":"JWT","alg":"HS512"}"""
          val payload = Json.parse(Base64.decodeBase64(split(1)).map(_.toChar).mkString)
          payload.\("aud").as[String] mustBe "testClientId"
          payload.\("iss").as[String] mustBe "testIssuer"
          payload.\("sub").as[String] mustBe "testUserId"
          payload.\("username").as[String] mustBe "test-org"
          payload.\("email").as[String] mustBe "test@email.com"
          payload.\("act").as[String] mustBe "organisation"
          payload.\("tsid").as[String] mustBe "testSetId"
          payload.\("tid").as[String] mustBe "testTokenId"
        }
      }
    }
  }

  "createClientAccessToken" should {
    "return a signed access token" when {
      "given a clientId" in {
        assertOutput(testService.createClientAccessToken("testClientId", "testSetId", "testTokenId")) { token =>
          val split = token.split("\\.")
          split.length mustBe 3

          Base64.decodeBase64(split(0)).map(_.toChar).mkString mustBe """{"typ":"JWT","alg":"HS512"}"""
          val payload = Json.parse(Base64.decodeBase64(split(1)).map(_.toChar).mkString)
          payload.\("aud").as[String] mustBe "testClientId"
          payload.\("iss").as[String] mustBe "testIssuer"
          payload.\("sub").as[String] mustBe "testClientId"
          payload.\("tsid").as[String] mustBe "testSetId"
          payload.\("tid").as[String] mustBe "testTokenId"
        }
      }
    }
  }

  "createTokenRecordSet" should {
    "return a MongoSuccessCreate" when {
      "the set has been created" in {
        mockCreateTokenRecord(success = true)

        awaitAndAssert(testService.createTokenRecordSet(
          tokenRecordSet.tokenSetId,
          tokenRecordSet.userId,
          tokenRecordSet.appId,
          tokenRecordSet.accessTokenId,
          tokenRecordSet.idTokenId,
          tokenRecordSet.refreshTokenId
        )) {
          _ mustBe MongoSuccessCreate
        }
      }
    }

    "return a MongoFailedCreate" when {
      "there was a problem creating the set" in {
        mockCreateTokenRecord(success = false)

        awaitAndAssert(testService.createTokenRecordSet(
          tokenRecordSet.tokenSetId,
          tokenRecordSet.userId,
          tokenRecordSet.appId,
          tokenRecordSet.accessTokenId,
          tokenRecordSet.idTokenId,
          tokenRecordSet.refreshTokenId
        )) {
          _ mustBe MongoFailedCreate
        }
      }
    }
  }

  "lookupTokenRecordSet" should {
    "return a TokenRecordSet" when {
      "the set has been found" in {
        mockValidateTokenRecord(record = Some(tokenRecordSet))

        awaitAndAssert(testService.lookupTokenRecordSet(tokenRecordSet.tokenSetId, tokenRecordSet.userId, tokenRecordSet.appId)) {
          _ mustBe Some(tokenRecordSet)
        }
      }
    }

    "return a MongoFailedCreate" when {
      "there was a problem creating the set" in {
        mockValidateTokenRecord(record = None)

        awaitAndAssert(testService.lookupTokenRecordSet(tokenRecordSet.tokenSetId, tokenRecordSet.userId, tokenRecordSet.appId)) {
          _ mustBe None
        }
      }
    }
  }
}
