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

import database.LoginAttemptStore
import dev.cjww.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoSuccessCreate}
import models.LoginAttempt
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.mongodb.scala.bson.conversions.Bson
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}

trait MockLoginAttemptStore extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockLoginAttemptStore: LoginAttemptStore = mock[LoginAttemptStore]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockLoginAttemptStore)
  }

  def mockCreateLoginAttempt(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockLoginAttemptStore.createLoginAttempt(ArgumentMatchers.any[LoginAttempt]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockValidateLoginAttempt(attempt: Option[LoginAttempt]): OngoingStubbing[Future[Option[LoginAttempt]]] = {
    when(mockLoginAttemptStore.validateLoginAttempt(ArgumentMatchers.any[Bson]()))
      .thenReturn(Future.successful(attempt))
  }
}
