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

import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoFailedUpdate, MongoSuccessCreate, MongoSuccessUpdate, MongoUpdatedResponse}
import models.TokenRecord
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.TokenService

import scala.concurrent.Future

trait MockTokenService extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockTokenService: TokenService = mock[TokenService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTokenService)
  }

  def mockCreateAccessToken(): OngoingStubbing[String] = {
    when(mockTokenService.createAccessToken(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[Long]()))
      .thenReturn("testAccessToken")
  }

  def mockCreateClientAccessToken(): OngoingStubbing[String] = {
    when(mockTokenService.createClientAccessToken(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[Long]()))
      .thenReturn("testAccessToken")
  }

  def mockCreateIdToken(): OngoingStubbing[String] = {
    when(mockTokenService.createIdToken(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[Map[String, String]](), ArgumentMatchers.any[Long]()))
      .thenReturn("testIdToken")
  }

  def mockCreateRefreshToken(): OngoingStubbing[String] = {
    when(mockTokenService.createRefreshToken(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[Long](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[Seq[String]]()))
      .thenReturn("testRefreshToken")
  }

  def mockUpdateTokenRecordSet(success: Boolean): OngoingStubbing[Future[MongoUpdatedResponse]] = {
    when(mockTokenService.updateTokenRecordSet(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(if(success) Future.successful(MongoSuccessUpdate) else Future.successful(MongoFailedUpdate))
  }

  def getMockExpiry(expiry: Long): OngoingStubbing[Long] = {
    when(mockTokenService.expiry)
      .thenReturn(expiry)
  }

  def mockGenerateTokenRecordSetId(): OngoingStubbing[String] = {
    when(mockTokenService.generateTokenRecordSetId)
      .thenReturn("testSetId")
  }

  def mockCreateTokenRecordSet(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockTokenService.createTokenRecordSet(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[Option[String]](), ArgumentMatchers.any[Option[String]]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockLookupTokenRecordSet(recordSet: Option[TokenRecord]): OngoingStubbing[Future[Option[TokenRecord]]] = {
    when(mockTokenService.lookupTokenRecordSet(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(recordSet))
  }

  def mockGetActiveSessionsFor(sessions: Seq[TokenRecord]): OngoingStubbing[Future[Seq[TokenRecord]]] = {
    when(mockTokenService.getActiveSessionsFor(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(sessions))
  }
}
