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
import models.User
import models.User._
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.ExecutionContext.Implicits.global

class IndividualUserStoreISpec extends PlaySpec with GuiceOneAppPerSuite with FutureAwaits with DefaultAwaitTimeout with BeforeAndAfterAll {

  val testUserStore: IndividualUserStore = app.injector.instanceOf[IndividualUserStore]

  val testUser: User = User(
    id       = "testUserId",
    userName = "testUserName",
    email    = "test@email.com",
    accType  = "INDIVIDUAL",
    password = "testPassword"
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
        val res = await(testUserStore.createUser(testUser))
        res mustBe MongoSuccessCreate
      }
    }
  }

  "validateUserOn" should {
    "return a User" when {
      "a user already exists with a matching email" in {
        val res = await(testUserStore.validateUserOn("email", testUser.email))
        res mustBe Some(testUser)
      }

      "a user already exists with a matching user name" in {
        val res = await(testUserStore.validateUserOn("userName", testUser.userName))
        res mustBe Some(testUser)
      }
    }

    "return None" when {
      "a user doesn't exist with the given email" in {
        val res = await(testUserStore.validateUserOn("email", "test-user@email.com"))
        res mustBe None
      }

      "a user doesn't exist with the given user name" in {
        val res = await(testUserStore.validateUserOn("userName", "otherTestUser"))
        res mustBe None
      }
    }
  }
}
