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

package helpers.services

import com.amazonaws.services.simpleemail.model.SendEmailResult
import models.Verification
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Request
import services.comms.email.SesService

import scala.concurrent.Future

trait MockEmailService extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockEmailService: SesService = mock[SesService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailService)
  }

  def mockSendEmailVerificationMessage(): OngoingStubbing[SendEmailResult] = {
    val result = new SendEmailResult().withMessageId("testMessageId")
    when(mockEmailService.sendEmailVerificationMessage(ArgumentMatchers.any[String](), ArgumentMatchers.any[Verification]())(ArgumentMatchers.any[Request[_]]()))
      .thenReturn(result)
  }

  def mockSaveVerificationRecord(verificationRecord: Option[Verification]): OngoingStubbing[Future[Option[Verification]]] = {
    when(mockEmailService.saveVerificationRecord(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(verificationRecord))
  }
}
