/*
 * Copyright 2019 CJWW Development
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

import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoFailedDelete, MongoSuccessCreate, MongoSuccessDelete}
import models.RegisteredApplication._
import models.{ClientTypes, RegisteredApplication}
import org.mongodb.scala.model.Filters.{equal => bsonEqual}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import utils.IntegrationSpec

class RegisteredApplicationsRepositoryISpec extends IntegrationSpec with GuiceOneAppPerSuite {

  val testRepo = app.injector.instanceOf[RegisteredApplicationsStore]

  val testApplicationOne = RegisteredApplication(
    name         = "testApp1",
    desc         = "testDesc1",
    homeUrl      = "/test/url",
    redirectUrl  = "/test/url/redirect",
    clientType   = ClientTypes.confidential.toString,
    clientId     = "testClientId1",
    clientSecret = Some("testClientSecret1")
  )

  val testApplicationTwo = RegisteredApplication(
    name         = null,
    desc         = "testDesc2",
    homeUrl      = "/test/url",
    redirectUrl  = "/test/url/redirect",
    clientType   = ClientTypes.confidential.toString,
    clientId     = "testClientId2",
    clientSecret = Some("testClientSecret2")
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(testRepo.collection[RegisteredApplication].deleteOne(bsonEqual("name", testApplicationOne.name)).toFuture())
  }

  override def afterEach(): Unit = {
    super.afterAll()
    await(testRepo.collection[RegisteredApplication].deleteOne(bsonEqual("name", testApplicationOne.name)).toFuture())
    await(testRepo.collection[RegisteredApplication].deleteOne(bsonEqual("name", testApplicationTwo.name)).toFuture())
  }

  "insertNewApplication" should {
    "return a MongoSuccessCreate" when {
      "a new application is successfully registered" in {
        awaitAndAssert(testRepo.insertNewApplication(testApplicationOne)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }

    "return a MongoFailedCreate" when {
      "a duplicate record is inserted" in {
        await(testRepo.insertNewApplication(testApplicationOne))

        awaitAndAssert(testRepo.insertNewApplication(testApplicationOne)) {
          _ mustBe MongoFailedCreate
        }
      }

      "a record is inserted containing a null value" in {
        awaitAndAssert(testRepo.insertNewApplication(testApplicationTwo)) {
          _ mustBe MongoFailedCreate
        }
      }
    }
  }

  "getApplication" should {
    "return an application" when {
      "a matching database row has been matched" in {
        await(testRepo.insertNewApplication(testApplicationOne))

        awaitAndAssert(testRepo.getOneApplication("name", testApplicationOne.name)) {
          _ mustBe Some(testApplicationOne)
        }
      }
    }

    "return a failed read" when {
      "no matching database row could be found" in {
        awaitAndAssert(testRepo.getOneApplication("name", testApplicationOne.name)) {
          _ mustBe None
        }
      }
    }
  }

  "removeRegisteredApplication" should {
    "return a MySQLSuccessDelete" when {
      "a row has been successfully deleted" in {
        await(testRepo.insertNewApplication(testApplicationOne))

        awaitAndAssert(testRepo.removeRegisteredApplication(testApplicationOne.name)) {
          _ mustBe MongoSuccessDelete
        }
      }
    }

    "return a MySQLFailedDelete" when {
      "a row has been successfully deleted" in {
        awaitAndAssert(testRepo.removeRegisteredApplication(testApplicationOne.name)) {
          _ mustBe MongoFailedDelete
        }
      }
    }
  }
}
