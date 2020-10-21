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

import com.cjwwdev.mongo.responses.{MongoSuccessCreate, MongoSuccessUpdate}
import helpers.{Assertions, IntegrationApp}
import models.{AuthorisedClient, User}
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{and => mongoAnd, equal => mongoEqual}
import org.mongodb.scala.model.Updates.set
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class IndividualUserStoreISpec extends PlaySpec with IntegrationApp with Assertions with BeforeAndAfterAll with CodecReg {

  val testUserStore: IndividualUserStore = app.injector.instanceOf[IndividualUserStore]

  val now: DateTime = DateTime.now()

  val testUser: User = User(
    id        = "testUserId",
    userName  = "testUserName",
    email     = "test@email.com",
    emailVerified = true,
    accType   = "individual",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = now
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(testUserStore.collection[User].drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(testUserStore.collection[User].drop().toFuture())
  }

  "createUser" should {
    "return a MongoSuccessCreate" when {
      "a new user has been created" in {
        awaitAndAssert(testUserStore.createUser(testUser)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }
  }

  "findUser" should {
    "return a User" when {
      "a user already exists with a matching email" in {
        awaitAndAssert(testUserStore.findUser(mongoEqual("email", testUser.email))) {
          _ mustBe Some(testUser)
        }
      }

      "a user already exists with a matching user name" in {
        awaitAndAssert(testUserStore.findUser(mongoEqual("userName", testUser.userName))) {
          _ mustBe Some(testUser)
        }
      }

      "a user matches on more than one field" in {
        val query = mongoAnd(mongoEqual("userName", testUser.userName), mongoEqual("email", testUser.email))
        awaitAndAssert(testUserStore.findUser(query)) {
          _ mustBe Some(testUser)
        }
      }
    }

    "return None" when {
      "a user doesn't exist with the given email" in {
        awaitAndAssert(testUserStore.findUser(mongoEqual("email", "test-user@email.com"))) {
          _ mustBe None
        }
      }

      "a user doesn't exist with the given user name" in {
        awaitAndAssert(testUserStore.findUser(mongoEqual("userName", "otherTestUser"))) {
          _ mustBe None
        }
      }

      "a user matches on one field" in {
        val query = mongoAnd(mongoEqual("userName", testUser.userName), mongoEqual("email", "invalid@email.com"))
        awaitAndAssert(testUserStore.findUser(query)) {
          _ mustBe None
        }
      }
    }
  }

  "projectValue" should {
    "return an Optional value" when {
      "projecting the email" in {
        awaitAndAssert(testUserStore.projectValue("userName", testUser.userName, "email")) { map =>
          map.get("email").map(_.asString().getValue) mustBe Some(testUser.email)
          map.get("id").map(_.asString().getValue) mustBe Some(testUser.id)
        }
      }

      "projecting the account type" in {
        awaitAndAssert(testUserStore.projectValue("userName", testUser.userName, "accType")) { map =>
          map.get("accType").map(_.asString().getValue) mustBe Some(testUser.accType)
          map.get("id").map(_.asString().getValue) mustBe Some(testUser.id)
        }
      }

      "projecting the date" in {
        awaitAndAssert(testUserStore.projectValue("userName", testUser.userName, "createdAt")) { map =>
          map.get("createdAt").map(_.asDateTime().getValue) mustBe Some(now.getMillis)
          map.get("id").map(_.asString().getValue) mustBe Some(testUser.id)
        }
      }
    }

    "return None" when {
      "the document doesn't exist" in {
        awaitAndAssert(testUserStore.projectValue("userName", "invalidUserName", "email")) {
          _ mustBe Map()
        }
      }

      "the field doesn't exist" in {
        awaitAndAssert(testUserStore.projectValue("userName", testUser.userName, "invalidField")) { map =>
          map.get("id").map(_.asString().getValue) mustBe Some(testUser.id)
        }
      }
    }
  }

  "updateUser" should {
    "return a MongoSuccessUpdate" when {
      "the user has been updated" in {
        awaitAndAssert(testUserStore.findUser(mongoEqual("userName", testUser.userName))) {
          _.get.authorisedClients mustBe List()
        }

        awaitAndAssert(testUserStore.updateUser(mongoEqual("userName", testUser.userName), set("authorisedClients", Seq(AuthorisedClient("testAppId", authorisedScopes = Seq(), authorisedOn = now))))) {
          _ mustBe MongoSuccessUpdate
        }

        awaitAndAssert(testUserStore.findUser(mongoEqual("userName", testUser.userName))) {
          _.get.authorisedClients mustBe Seq(AuthorisedClient("testAppId", authorisedScopes = Seq(), authorisedOn = now))
        }
      }
    }
  }
}
