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
import helpers.Assertions
import helpers.aws.MockSES
import helpers.database.MockVerificationStore
import models.Verification
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.comms.email.SesService

import scala.concurrent.ExecutionContext.Implicits.global

class SesServiceSpec
  extends PlaySpec
    with Assertions
    with MockVerificationStore
    with MockSES {

  private val testService: SesService = new SesService {
    override val awsRegion: String = "eu-west-2"
    override val emailClient: AmazonSimpleEmailService = mockSES
    override val emailSenderAddress: String = "test@email.com"
    override val verificationSubjectLine: String = "test verification line"
    override val verificationStore: VerificationStore = mockVerificationStore
  }

  "sendEmailVerificationMessage" should {
    "return an EmailResponse" when {
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

        awaitAndAssert(testService.sendEmailVerificationMessage("test@email.com", testVerificationRecord)) {
          _.messageId mustBe "testMessageId"
        }
      }
    }
  }
}
