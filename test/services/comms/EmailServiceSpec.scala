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

package services.comms

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import database.VerificationStore
import dev.cjww.mongo.responses.{MongoFailedDelete, MongoSuccessDelete}
import helpers.Assertions
import helpers.aws.MockSES
import helpers.database.MockVerificationStore
import models.Verification
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global

class EmailServiceSpec
  extends PlaySpec
    with Assertions
    with MockVerificationStore
    with MockSES {

  private val testService: EmailService = new EmailService {
    override val emailSenderAddress: String = "test@email.com"
    override val verificationSubjectLine: String = "Test subject line"
    override val emailClient: AmazonSimpleEmailService = mockSES
    override val verificationStore: VerificationStore = mockVerificationStore
  }

  "sendEmailVerificationMessage" should {
    "return a SendEmailResult" when {
      "the verification email has been sent" in {
        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

        val testVerificationRecord = Verification(
          verificationId = "test-verify-id",
          userId = "test-user-id",
          contactType = "email",
          contact = "test@email.com",
          code = None,
          accType = "individual",
          createdAt = new DateTime()
        )

        mockSendEmail()

        assertOutput(testService.sendEmailVerificationMessage("test@email.com", testVerificationRecord)) {
          _.getMessageId mustBe "testMessageId"
        }
      }
    }
  }

  "saveVerificationRecord" should {
    "return a verification record" when {
      "the record has been saved" in {
        mockCreateEmailVerificationRecord(success = true)

        awaitAndAssert(testService.saveVerificationRecord("testUserId", "testEmail", "testAccType")) { record =>
          assert(record.get.verificationId.startsWith("verify-"))
          record.get.userId mustBe "testUserId"
          record.get.contactType mustBe "email"
          record.get.contact mustBe "testEmail"
          record.get.code mustBe None
          record.get.accType mustBe "testAccType"
        }
      }
    }

    "return none" when {
      "there was a problem saving the verification record" in {
        mockCreateEmailVerificationRecord(success = false)

        awaitAndAssert(testService.saveVerificationRecord("testUserId", "testEmail", "testAccType")) {
          _ mustBe None
        }
      }
    }
  }

  "validateVerificationRecord" should {
    val record = Verification(
      verificationId = "testVerificationId",
      userId = "testUserId",
      contactType = "testContactMedium",
      contact = "testContactMedium",
      code = None,
      accType = "testAccType",
      createdAt = DateTime.now()
    )

    "return a verification record" when {
      "a verification record was found" in {
        mockValidateTokenRecord(record = Some(record))

        awaitAndAssert(testService.validateVerificationRecord(record)) {
          _ mustBe Some(record)
        }
      }
    }

    "return none" when {
      "the verification record could not be found" in {
        mockValidateTokenRecord(record = None)

        awaitAndAssert(testService.validateVerificationRecord(record)) {
          _ mustBe None
        }
      }
    }
  }

  "removeVerificationRecord" should {
    "return MongoSuccessDelete" when {
      "the record has been removed" in {
        mockDeleteEmailVerificationRecord(success = true)

        awaitAndAssert(testService.removeVerificationRecord("testVerificationId")) {
          _ mustBe MongoSuccessDelete
        }
      }
    }

    "return MongoFailedDelete" when {
      "the record has not been removed" in {
        mockDeleteEmailVerificationRecord(success = false)

        awaitAndAssert(testService.removeVerificationRecord("testVerificationId")) {
          _ mustBe MongoFailedDelete
        }
      }
    }
  }
}
