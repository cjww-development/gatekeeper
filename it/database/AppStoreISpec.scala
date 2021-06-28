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

import dev.cjww.mongo.responses.{MongoSuccessCreate, MongoSuccessDelete, MongoSuccessUpdate}
import helpers.{Assertions, IntegrationApp}
import models.{RegisteredApplication, User}
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.mongodb.scala.model.Updates.set
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global

class AppStoreISpec extends PlaySpec with IntegrationApp with Assertions with BeforeAndAfterAll with CodecReg {

  val testAppStore: AppStore = app.injector.instanceOf[AppStore]

  val now = DateTime.now()

  val testApp: RegisteredApplication = RegisteredApplication(
    appId        = "testAppId",
    owner        = "testOwner",
    name         = "test-app-name",
    desc         = "test desc",
    iconUrl      = None,
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/rediect",
    clientType   = "confidential",
    clientId     = "testClientId",
    clientSecret = Some("testClientSecret"),
    oauth2Flows = Seq(),
    oauth2Scopes = Seq(),
    idTokenExpiry = 0L,
    accessTokenExpiry = 0L,
    refreshTokenExpiry = 0L,
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

  "getAppsOwnedBy" should {
    "return a Sequence of apps" when {
      "there are apps owned by the user" in {
        awaitAndAssert(testAppStore.getAppsOwnedBy(testApp.owner)) {
          _ mustBe Seq(testApp)
        }
      }
    }

    "return an empty Sequence of apps" when {
      "there are no apps owned by the user" in {
        awaitAndAssert(testAppStore.getAppsOwnedBy("invalid-owner")) {
          _ mustBe Seq()
        }
      }
    }
  }

  "updateApp" should {
    "return a MongoSuccessUpdate" when {
      "the app has been updated" in {
        awaitAndAssert(testAppStore.validateAppOn(mongoEqual("clientId", testApp.clientId))) {
          _.get.desc mustBe testApp.desc
        }

        awaitAndAssert(testAppStore.updateApp(mongoEqual("clientId", testApp.clientId), set("desc", "new set desc"))) {
          _ mustBe MongoSuccessUpdate
        }

        awaitAndAssert(testAppStore.validateAppOn(mongoEqual("clientId", testApp.clientId))) {
          _.get.desc mustBe "new set desc"
        }
      }
    }
  }

  "deleteApp" should {
    "return a MongoSuccessDelete" when {
      "the app has been deleted" in {
        awaitAndAssert(testAppStore.deleteApp(mongoEqual("clientId", testApp.clientId))) {
          _ mustBe MongoSuccessDelete
        }
      }
    }
  }
}
