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

package database

import dev.cjww.mongo.responses.MongoSuccessCreate
import helpers.{Assertions, IntegrationApp}
import models.Grant
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class GrantStoreISpec extends PlaySpec with IntegrationApp with Assertions with BeforeAndAfterAll with CodecReg {

  val testGrantStore: GrantStore = app.injector.instanceOf[GrantStore]

  val now = DateTime.now()

  val testGrant: Grant = Grant(
    responseType = "code",
    authCode = "testAuthCode",
    scope = Seq("testScope"),
    clientId = "testClientId",
    userId = "testUserId",
    accType = "testType",
    redirectUri = "testRedirect",
    codeVerifier = None,
    codeChallenge = None,
    codeChallengeMethod = None,
    createdAt = DateTime.now()
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(testGrantStore.collection[Grant].drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(testGrantStore.collection[Grant].drop().toFuture())
  }

  "createGrant" should {
    "return a MongoSuccessCreate" when {
      "a new grant has been created" in {
        awaitAndAssert(testGrantStore.createGrant(testGrant)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }
  }

  "validateGrant" should {
    "return a Grant" when {
      "matching both the auth code and state" in {
        val query = mongoEqual("authCode", testGrant.authCode)

        awaitAndAssert(testGrantStore.validateGrant(query)) {
          _ mustBe Some(testGrant)
        }
      }
    }

    "return None" when {
      "an app doesn't exist with a matching clientId" in {
        val query = mongoEqual("authCode", "invalid-auth-code")

        awaitAndAssert(testGrantStore.validateGrant(query)) {
          _ mustBe None
        }
      }
    }
  }
}
