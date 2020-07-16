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

import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoSuccessCreate}
import database.{AppStore, GrantStore}
import models.{Grant, RegisteredApplication}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}

trait MockGrantStore extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockGrantStore: GrantStore = mock[GrantStore]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGrantStore)
  }

  def mockCreateGrant(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockGrantStore.createGrant(ArgumentMatchers.any[Grant]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockValidateGrant(app: Option[Grant]): OngoingStubbing[Future[Option[Grant]]] = {
    when(mockGrantStore.validateGrant(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(app))
  }
}
