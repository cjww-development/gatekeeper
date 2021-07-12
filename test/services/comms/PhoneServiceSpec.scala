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

import com.amazonaws.services.sns.AmazonSNS
import database.VerificationStore
import dev.cjww.mongo.responses.{MongoFailedDelete, MongoSuccessDelete}
import helpers.Assertions
import helpers.aws.MockSNS
import helpers.database.MockVerificationStore
import models.Verification
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class PhoneServiceSpec
  extends PlaySpec
    with Assertions
    with MockVerificationStore
    with MockSNS {

  private val testService: PhoneService = new PhoneService {
    override protected val snsClient: AmazonSNS = mockSNS
    override protected val verifyMessage: String = "testVerifyMessage"
    override protected val verifySenderId: String = "testSenderId"
    override protected val verifyMsgType: String = "testMessageType"
    override protected val maxPrice: String = "1"
    override protected val verificationStore: VerificationStore = mockVerificationStore
  }

  "sendSMSVerification" should {
    "return a PublishResult" when {
      "the sms message has been sent" in {
        mockSNSPublish()

        assertOutput(testService.sendSMSVerification("testPhoneNumber", "testCode")) {
          _.getMessageId mustBe "testMessageId"
        }
      }
    }
  }

  "saveVerificationRecord" should {
    "return a verification record" when {
      "the record has been saved" in {
        mockCreateEmailVerificationRecord(success = true)

        awaitAndAssert(testService.saveVerificationRecord("testUserId", "testPhoneNumber", "testAccType")) { record =>
          assert(record.get.verificationId.startsWith("verify-"))
          record.get.userId mustBe "testUserId"
          record.get.contactType mustBe "phone"
          record.get.contact mustBe "testPhoneNumber"
          record.get.code.get.length mustBe 6
          record.get.accType mustBe "testAccType"
        }
      }
    }

    "return none" when {
      "there was a problem saving the verification record" in {
        mockCreateEmailVerificationRecord(success = false)

        awaitAndAssert(testService.saveVerificationRecord("testUserId", "testPhoneNumber", "testAccType")) {
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

        awaitAndAssert(testService.validateVerificationRecord("testUserId", "testCode")) {
          _ mustBe Some(record)
        }
      }
    }

    "return none" when {
      "the verification record could not be found" in {
        mockValidateTokenRecord(record = None)

        awaitAndAssert(testService.validateVerificationRecord("testUserId", "testCode")) {
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
