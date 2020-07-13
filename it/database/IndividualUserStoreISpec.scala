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
import helpers.Assertions
import models.User
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.mongodb.scala.model.Filters.{equal => mongoEqual, and => mongoAnd}

import scala.concurrent.ExecutionContext.Implicits.global

class IndividualUserStoreISpec extends PlaySpec with GuiceOneAppPerSuite with Assertions with BeforeAndAfterAll with CodecReg {

  val testUserStore: IndividualUserStore = app.injector.instanceOf[IndividualUserStore]

  val now = DateTime.now()

  val testUser: User = User(
    id        = "testUserId",
    userName  = "testUserName",
    email     = "test@email.com",
    accType   = "individual",
    password  = "testPassword",
    salt      = "testSalt",
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

  "validateUserOn" should {
    "return a User" when {
      "a user already exists with a matching email" in {
        awaitAndAssert(testUserStore.validateUserOn(mongoEqual("email", testUser.email))) {
          _ mustBe Some(testUser)
        }
      }

      "a user already exists with a matching user name" in {
        awaitAndAssert(testUserStore.validateUserOn(mongoEqual("userName", testUser.userName))) {
          _ mustBe Some(testUser)
        }
      }

      "a user matches on more than one field" in {
        val query = mongoAnd(mongoEqual("userName", testUser.userName), mongoEqual("email", testUser.email))
        awaitAndAssert(testUserStore.validateUserOn(query)) {
          _ mustBe Some(testUser)
        }
      }
    }

    "return None" when {
      "a user doesn't exist with the given email" in {
        awaitAndAssert(testUserStore.validateUserOn(mongoEqual("email", "test-user@email.com"))) {
          _ mustBe None
        }
      }

      "a user doesn't exist with the given user name" in {
        awaitAndAssert(testUserStore.validateUserOn(mongoEqual("userName", "otherTestUser"))) {
          _ mustBe None
        }
      }

      "a user matches on one field" in {
        val query = mongoAnd(mongoEqual("userName", testUser.userName), mongoEqual("email", "invalid@email.com"))
        awaitAndAssert(testUserStore.validateUserOn(query)) {
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
}
