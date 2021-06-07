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

import dev.cjww.mongo.responses.MongoDeleteResponse
import models.RegisteredApplication
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.{ClientService, RegenerationResponse}

import scala.concurrent.Future

trait MockClientService extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockClientService: ClientService = mock[ClientService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockClientService)
  }

  def mockGetRegisteredApp(app: Option[RegisteredApplication]): OngoingStubbing[Future[Option[RegisteredApplication]]] = {
    when(mockClientService.getRegisteredApp(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(app))
  }

  def mockGetRegisteredAppByAppId(app: Option[RegisteredApplication]): OngoingStubbing[Future[Option[RegisteredApplication]]] = {
    when(mockClientService.getRegisteredApp(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(app))
  }

  def mockGetRegisteredAppByIdAndSecret(app: Option[RegisteredApplication]): OngoingStubbing[Future[Option[RegisteredApplication]]] = {
    when(mockClientService.getRegisteredAppByIdAndSecret(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(app))
  }

  def mockGetRegisteredAppById(app: Option[RegisteredApplication]): OngoingStubbing[Future[Option[RegisteredApplication]]] = {
    when(mockClientService.getRegisteredAppById(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(app))
  }

  def mockGetRegisteredApps(apps: Seq[RegisteredApplication]): OngoingStubbing[Future[Seq[RegisteredApplication]]] = {
    when(mockClientService.getRegisteredAppsFor(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(apps))
  }

  def mockRegenerateClientIdAndSecret(resp: RegenerationResponse): OngoingStubbing[Future[RegenerationResponse]] = {
    when(mockClientService.regenerateClientIdAndSecret(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[Boolean]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }

  def mockDeleteClient(resp: MongoDeleteResponse): OngoingStubbing[Future[MongoDeleteResponse]] = {
    when(mockClientService.deleteClient(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }
}
