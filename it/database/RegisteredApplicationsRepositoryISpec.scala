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

import database.responses._
import models.{ClientTypes, RegisteredApplication}
import slick.dbio.Effect
import slick.jdbc.MySQLProfile.api._
import slick.sql.FixedSqlAction
import utils.IntegrationSpec

import scala.concurrent.ExecutionContext.Implicits.global

class RegisteredApplicationsRepositoryISpec extends IntegrationSpec {

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

    await(testRepo.getDb.run(testRepo.table.schema.create))

    val deleteQuery: String => FixedSqlAction[Int, NoStream, Effect.Write] =
      name => testRepo.table.filter(_.name === name).delete

    await(testRepo.getDb.run(deleteQuery(testApplicationOne.name)))
  }

  override def afterEach(): Unit = {
    super.afterAll()
    await(testRepo.getDb.run(testRepo.table.schema.drop))
  }

  "insertNewApplication" should {
    "return a MySQLSuccessCreate" when {
      "a new application is successfully registered" in {
        awaitAndAssert(testRepo.insertNewApplication(testApplicationOne)) {
          _ mustBe MySQLSuccessCreate
        }
      }
    }

    "return a MySQLFailedCreate" when {
      "a duplicate record is inserted" in {
        await(testRepo.insertNewApplication(testApplicationOne))

        awaitAndAssert(testRepo.insertNewApplication(testApplicationOne)) {
          _ mustBe MySQLFailedCreate
        }
      }

      "a record is inserted containing a null value" in {
        awaitAndAssert(testRepo.insertNewApplication(testApplicationTwo)) {
          _ mustBe MySQLFailedCreate
        }
      }
    }
  }

  "getApplication" should {
    "return an application" when {
      "a matching database row has been matched" in {
        await(testRepo.insertNewApplication(testApplicationOne))

        awaitAndAssert(testRepo.getOneApplication(testRepo.table.filter(_.name === testApplicationOne.name).result.headOption)) {
          _ mustBe Right(testApplicationOne)
        }
      }
    }

    "return a failed read" when {
      "no matching database row could be found" in {
        awaitAndAssert(testRepo.getOneApplication(testRepo.table.filter(_.name === testApplicationOne.name).result.headOption)) {
          _ mustBe Left(MySQLFailedRead)
        }
      }
    }
  }

  "removeRegisteredApplication" should {
    "return a MySQLSuccessDelete" when {
      "a row has been successfully deleted" in {
        await(testRepo.insertNewApplication(testApplicationOne))

        awaitAndAssert(testRepo.removeRegisteredApplication(testApplicationOne.name)) {
          _ mustBe MySQLSuccessDelete
        }
      }
    }

    "return a MySQLFailedDelete" when {
      "a row has been successfully deleted" in {
        awaitAndAssert(testRepo.removeRegisteredApplication(testApplicationOne.name)) {
          _ mustBe MySQLFailedDelete
        }
      }
    }
  }
}
