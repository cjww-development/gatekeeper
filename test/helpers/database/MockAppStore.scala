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

import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoDeleteResponse, MongoFailedCreate, MongoSuccessCreate, MongoUpdatedResponse}
import database.AppStore
import models.RegisteredApplication
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.mongodb.scala.bson.conversions.Bson
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}

trait MockAppStore extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockAppStore: AppStore = mock[AppStore]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppStore)
  }

  def mockCreateApp(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockAppStore.createApp(ArgumentMatchers.any[RegisteredApplication]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockValidateAppOn(app: Option[RegisteredApplication]): OngoingStubbing[Future[Option[RegisteredApplication]]] = {
    when(mockAppStore.validateAppOn(ArgumentMatchers.any[Bson]()))
      .thenReturn(Future.successful(app))
  }

  def mockMultipleValidateAppOn(appOne: Option[RegisteredApplication],
                                appTwo: Option[RegisteredApplication],
                                appThree: Option[RegisteredApplication],
                                appFour: Option[RegisteredApplication]): OngoingStubbing[Future[Option[RegisteredApplication]]] = {
    when(mockAppStore.validateAppOn(ArgumentMatchers.any[Bson]()))
      .thenReturn(Future.successful(appOne))
      .thenReturn(Future.successful(appTwo))
      .thenReturn(Future.successful(appThree))
      .thenReturn(Future.successful(appFour))
  }

  def mockGetAppsOwnedBy(apps: Seq[RegisteredApplication]): OngoingStubbing[Future[Seq[RegisteredApplication]]] = {
    when(mockAppStore.getAppsOwnedBy(ArgumentMatchers.any[String]()))
      .thenReturn(Future.successful(apps))
  }

  def mockUpdateApp(resp: MongoUpdatedResponse): OngoingStubbing[Future[MongoUpdatedResponse]] = {
    when(mockAppStore.updateApp(ArgumentMatchers.any[Bson](), ArgumentMatchers.any[Bson]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }

  def mockDeleteApp(resp: MongoDeleteResponse): OngoingStubbing[Future[MongoDeleteResponse]] = {
    when(mockAppStore.deleteApp(ArgumentMatchers.any[Bson]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }
}
