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

import com.cjwwdev.mongo.responses.{MongoSuccessCreate, MongoSuccessDelete}
import helpers.{Assertions, IntegrationApp}
import models.Verification
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class VerificationStoreISpec extends PlaySpec with IntegrationApp with Assertions with BeforeAndAfterAll with CodecReg {

  val testVerificationStore: VerificationStore = app.injector.instanceOf[VerificationStore]

  val now: DateTime = DateTime.now()

  val testEmailVerification: Verification = Verification(
    verificationId = "testVerificationId",
    userId = "testUserId",
    contactType = "email",
    contact = "test@email.com",
    code = None,
    accType = "organisation",
    createdAt = now
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(testVerificationStore.collection[Verification].drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(testVerificationStore.collection[Verification].drop().toFuture())
  }

  "createEmailVerificationRecord" should {
    "return a MongoSuccessCreate" when {
      "a new email verification record has been created" in {
        awaitAndAssert(testVerificationStore.createVerificationRecord(testEmailVerification)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }
  }

  "validateEmailVerificationRecord" should {
    "return an Email verification record" when {
      "matching the verification id" in {
        val query = mongoEqual("verificationId", testEmailVerification.verificationId)

        awaitAndAssert(testVerificationStore.validateVerificationRecord(query)) {
          _ mustBe Some(testEmailVerification)
        }
      }
    }

    "return None" when {
      "the verification id is invalid" in {
        val query = mongoEqual("verificationId", "invalid=-id")

        awaitAndAssert(testVerificationStore.validateVerificationRecord(query)) {
          _ mustBe None
        }
      }
    }
  }

  "deleteEmailVerificationRecord" should {
    "return MongoSuccessDelete" when {
      "the record has been deleted" in {
        awaitAndAssert(testVerificationStore.deleteVerificationRecord(mongoEqual("userId", testEmailVerification.userId))) {
          _ mustBe MongoSuccessDelete
        }
      }
    }
  }
}
