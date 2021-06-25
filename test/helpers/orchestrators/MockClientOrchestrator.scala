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

package helpers.orchestrators

import models.RegisteredApplication
import orchestrators.ClientOrchestrator
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.Future

trait MockClientOrchestrator extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockClientOrchestrator: ClientOrchestrator = mock[ClientOrchestrator]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockClientOrchestrator)
  }

  def mockGetRegisteredApp(app: Option[RegisteredApplication]): OngoingStubbing[Future[Option[RegisteredApplication]]] = {
    when(mockClientOrchestrator.getRegisteredApp(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(app))
  }

  def mockGetRegisteredApps(apps: Seq[Seq[RegisteredApplication]]): OngoingStubbing[Future[Seq[Seq[RegisteredApplication]]]] = {
    when(mockClientOrchestrator.getRegisteredApps(ArgumentMatchers.any[String](), ArgumentMatchers.any[Int]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(apps))
  }
}
