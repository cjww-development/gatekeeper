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

package helpers.database

import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoDeleteResponse, MongoFailedCreate, MongoFailedDelete, MongoSuccessCreate, MongoSuccessDelete}
import database.{EmailVerificationStore, TokenRecordStore}
import models.{EmailVerification, TokenRecord}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.mongodb.scala.bson.conversions.Bson
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}

trait MockEmailVerificationStore extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockEmailVerificationStore: EmailVerificationStore = mock[EmailVerificationStore]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailVerificationStore)
  }

  def mockCreateEmailVerificationRecord(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockEmailVerificationStore.createEmailVerificationRecord(ArgumentMatchers.any[EmailVerification]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockValidateTokenRecord(record: Option[EmailVerification]): OngoingStubbing[Future[Option[EmailVerification]]] = {
    when(mockEmailVerificationStore.validateEmailVerificationRecord(ArgumentMatchers.any[Bson]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(record))
  }

  def mockDeleteEmailVerificationRecord(success: Boolean): OngoingStubbing[Future[MongoDeleteResponse]] = {
    when(mockEmailVerificationStore.deleteEmailVerificationRecord(ArgumentMatchers.any[Bson]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessDelete else MongoFailedDelete))
  }
}
