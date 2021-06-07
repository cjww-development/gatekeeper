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

import dev.cjww.mongo.responses._
import database.VerificationStore
import models.Verification
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.mongodb.scala.bson.conversions.Bson
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}

trait MockVerificationStore extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockVerificationStore: VerificationStore = mock[VerificationStore]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockVerificationStore)
  }

  def mockCreateEmailVerificationRecord(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockVerificationStore.createVerificationRecord(ArgumentMatchers.any[Verification]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockValidateTokenRecord(record: Option[Verification]): OngoingStubbing[Future[Option[Verification]]] = {
    when(mockVerificationStore.validateVerificationRecord(ArgumentMatchers.any[Bson]()))
      .thenReturn(Future.successful(record))
  }

  def mockDeleteEmailVerificationRecord(success: Boolean): OngoingStubbing[Future[MongoDeleteResponse]] = {
    when(mockVerificationStore.deleteVerificationRecord(ArgumentMatchers.any[Bson]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessDelete else MongoFailedDelete))
  }
}
