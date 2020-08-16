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
import models.{RegisteredApplication, User}
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.mongodb.scala.model.Filters.{and => mongoAnd, equal => mongoEqual}

import scala.concurrent.ExecutionContext.Implicits.global

class AppStoreISpec extends PlaySpec with IntegrationApp with Assertions with BeforeAndAfterAll with CodecReg {

  val testAppStore: AppStore = app.injector.instanceOf[AppStore]

  val now = DateTime.now()

  val testApp: RegisteredApplication = RegisteredApplication(
    appId        = "testAppId",
    owner        = "testOwner",
    name         = "test-app-name",
    desc         = "test desc",
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/rediect",
    clientType   = "confidential",
    clientId     = "testClientId",
    clientSecret = Some("testClientSecret"),
    createdAt    = DateTime.now()
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(testAppStore.collection[User].drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(testAppStore.collection[User].drop().toFuture())
  }

  "createUser" should {
    "return a MongoSuccessCreate" when {
      "a new user has been created" in {
        awaitAndAssert(testAppStore.createApp(testApp)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }
  }

  "validateAppOn" should {
    "return a RegisteredApplication" when {
      "an app already exists with a matching clientId" in {
        awaitAndAssert(testAppStore.validateAppOn(mongoEqual("clientId", testApp.clientId))) {
          _ mustBe Some(testApp)
        }
      }

      "an app already exists with a matching clientSecret" in {
        awaitAndAssert(testAppStore.validateAppOn(mongoEqual("clientSecret", testApp.clientSecret.get))) {
          _ mustBe Some(testApp)
        }
      }
    }

    "return None" when {
      "an app doesn't exist with a matching clientId" in {
        awaitAndAssert(testAppStore.validateAppOn(mongoEqual("clientId", "invalid-id"))) {
          _ mustBe None
        }
      }

      "an app doesn't exist with a matching clientSecret" in {
        awaitAndAssert(testAppStore.validateAppOn(mongoEqual("clientSecret", "invalid-secret"))) {
          _ mustBe None
        }
      }
    }
  }
}
