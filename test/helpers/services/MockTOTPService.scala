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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.security.{MFAEnabledResponse, QRCodeResponse, SecretResponse, TOTPService}

import scala.concurrent.Future

trait MockTOTPService extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockTOTPService: TOTPService = mock[TOTPService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTOTPService)
  }

  def mockGenerateSecret(resp: SecretResponse): OngoingStubbing[Future[SecretResponse]] = {
    when(mockTOTPService.generateSecret(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }

  def mockGetCurrentSecret(resp: SecretResponse): OngoingStubbing[Future[SecretResponse]] = {
    when(mockTOTPService.getCurrentSecret(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }

  def mockGenerateQRCode(resp: QRCodeResponse): OngoingStubbing[Future[QRCodeResponse]] = {
    when(mockTOTPService.generateQRCode(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }

  def mockValidateCodes(resp: Boolean): OngoingStubbing[Boolean] = {
    when(mockTOTPService.validateCodes(ArgumentMatchers.any[String](), ArgumentMatchers.any()))
      .thenReturn(resp)
  }

  def mockEnableAccountMFA(resp: MFAEnabledResponse): OngoingStubbing[Future[MFAEnabledResponse]] = {
    when(mockTOTPService.enableAccountMFA(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }

  def mockGetMFAStatus(resp: Boolean): OngoingStubbing[Future[Boolean]] = {
    when(mockTOTPService.getMFAStatus(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }

  def mockRemoveTOTPMFA(removed: Boolean): OngoingStubbing[Future[Boolean]] = {
    when(mockTOTPService.removeTOTPMFA(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(removed))
  }
}
