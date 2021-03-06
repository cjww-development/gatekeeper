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

import database.TokenRecordStore
import dev.cjww.mongo.responses._
import models.TokenRecord
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.mongodb.scala.bson.conversions.Bson
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}

trait MockTokenRecordStore extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockTokenRecordStore: TokenRecordStore = mock[TokenRecordStore]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTokenRecordStore)
  }

  def mockCreateTokenRecord(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockTokenRecordStore.createTokenRecord(ArgumentMatchers.any[TokenRecord]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockValidateTokenRecord(record: Option[TokenRecord]): OngoingStubbing[Future[Option[TokenRecord]]] = {
    when(mockTokenRecordStore.validateTokenRecord(ArgumentMatchers.any[Bson]()))
      .thenReturn(Future.successful(record))
  }

  def mockUpdateTokenRecord(success: Boolean): OngoingStubbing[Future[MongoUpdatedResponse]] = {
    when(mockTokenRecordStore.updateTokenRecord(ArgumentMatchers.any[Bson](), ArgumentMatchers.any[Bson]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(if(success) MongoSuccessUpdate else MongoFailedUpdate))
  }

  def mockDeleteTokenRecord(success: Boolean): OngoingStubbing[Future[MongoDeleteResponse]] = {
    when(mockTokenRecordStore.deleteTokenRecord(ArgumentMatchers.any[Bson]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(if(success) MongoSuccessDelete else MongoFailedDelete))
  }

  def mockGetActiveRecords(records: Seq[TokenRecord]): OngoingStubbing[Future[Seq[TokenRecord]]] = {
    when(mockTokenRecordStore.getActiveRecords(ArgumentMatchers.any[Bson]()))
      .thenReturn(Future.successful(records))
  }
}
