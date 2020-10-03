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

import java.util.UUID

import com.cjwwdev.mongo.responses.MongoSuccessCreate
import helpers.{Assertions, IntegrationApp}
import models.{Grant, TokenRecord}
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class TokenRecordStoreISpec extends PlaySpec with IntegrationApp with Assertions with BeforeAndAfterAll with CodecReg {

  val testTokenRecordStore: TokenRecordStore = app.injector.instanceOf[TokenRecordStore]

  val now: DateTime = DateTime.now()

  val testTokenRecord: TokenRecord = TokenRecord(
    tokenSetId = "testTokenSetId",
    userId = "testUserId",
    appId = "testAppId",
    issuedAt = now
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(testTokenRecordStore.collection[TokenRecord].drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(testTokenRecordStore.collection[TokenRecord].drop().toFuture())
  }

  "createTokenRecord" should {
    "return a MongoSuccessCreate" when {
      "a new token record has been created" in {
        awaitAndAssert(testTokenRecordStore.createTokenRecord(testTokenRecord)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }
  }

  "validateGrant" should {
    "return a Grant" when {
      "matching both the auth code and state" in {
        val query = mongoEqual("tokenSetId", testTokenRecord.tokenSetId)

        awaitAndAssert(testTokenRecordStore.validateTokenRecord(query)) {
          _ mustBe Some(testTokenRecord)
        }
      }
    }

    "return None" when {
      "an app doesn't exist with a matching clientId" in {
        val query = mongoEqual("tokenSetId", "invalid-id")

        awaitAndAssert(testTokenRecordStore.validateTokenRecord(query)) {
          _ mustBe None
        }
      }
    }
  }

  "getActiveRecords" should {
    "return a Seq of records" when {
      "there are active records matching the query" in {
        awaitAndAssert(testTokenRecordStore.getActiveRecords(mongoEqual("userId", testTokenRecord.userId))) {
          _ mustBe Seq(testTokenRecord)
        }
      }
    }
  }
}
