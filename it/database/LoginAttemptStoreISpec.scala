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

package database

import com.cjwwdev.mongo.responses.MongoSuccessCreate
import helpers.{Assertions, IntegrationApp}
import models.LoginAttempt
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class LoginAttemptStoreISpec extends PlaySpec with IntegrationApp with Assertions with BeforeAndAfterAll with CodecReg {

  val testLoginAttemptStore: LoginAttemptStore = app.injector.instanceOf[LoginAttemptStore]

  val testLoginAttempt: LoginAttempt = LoginAttempt(
    "testUserId",
    success = true
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(testLoginAttemptStore.collection[LoginAttempt].drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(testLoginAttemptStore.collection[LoginAttempt].drop().toFuture())
  }

  "createGrant" should {
    "return a MongoSuccessCreate" when {
      "a new grant has been created" in {
        awaitAndAssert(testLoginAttemptStore.createLoginAttempt(testLoginAttempt)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }
  }

  "validateGrant" should {
    "return a Grant" when {
      "matching both the auth code and state" in {
        val query = mongoEqual("id", testLoginAttempt.id)

        awaitAndAssert(testLoginAttemptStore.validateLoginAttempt(query)) {
          _ mustBe Some(testLoginAttempt)
        }
      }
    }

    "return None" when {
      "an app doesn't exist with a matching clientId" in {
        val query = mongoEqual("id", "invalid-attempt-id")

        awaitAndAssert(testLoginAttemptStore.validateLoginAttempt(query)) {
          _ mustBe None
        }
      }
    }
  }
}
