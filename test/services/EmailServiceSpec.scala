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

package services

import java.util.UUID

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import database.{EmailVerificationStore, LoginAttemptStore, UserStore}
import helpers.Assertions
import helpers.aws.MockSES
import helpers.database.{MockAppStore, MockEmailVerificationStore, MockIndividualStore, MockLoginAttemptStore, MockOrganisationStore}
import models.{EmailVerification, LoginAttempt, User}
import org.joda.time.DateTime
import org.mongodb.scala.bson.BsonString
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global

class EmailServiceSpec
  extends PlaySpec
    with Assertions
    with MockEmailVerificationStore
    with MockSES {

  private val testService: EmailService = new EmailService {
    override val emailSenderAddress: String = "test@email.com"
    override val verificationSubjectLine: String = "Test subject line"
    override val emailClient: AmazonSimpleEmailService = mockSES
    override val emailVerificationStore: EmailVerificationStore = mockEmailVerificationStore
  }

  "sendEmailVerificationMessage" should {
    "return a SendEmailResult" when {
      "the verification email has been sent" in {
        implicit val fakeRequest = FakeRequest()

        val testVerificationRecord = EmailVerification(
          verificationId = "test-verify-id",
          userId = "test-user-id",
          email = "test@email.com",
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
}
