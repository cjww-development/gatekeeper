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

import models.{Grant, RegisteredApplication}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.GrantService

import scala.concurrent.Future

trait MockGrantService extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockGrantService: GrantService = mock[GrantService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGrantService)
  }

  def mockGetRegisteredApp(app: Option[RegisteredApplication]): OngoingStubbing[Future[Option[RegisteredApplication]]] = {
    when(mockGrantService.getRegisteredApp(ArgumentMatchers.any[String]()))
      .thenReturn(Future.successful(app))
  }

  def mockValidateRedirectUrl(valid: Boolean): OngoingStubbing[Boolean] = {
    when(mockGrantService.validateRedirectUrl(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]()))
      .thenReturn(valid)
  }

  def mockValidateGrant(grant: Option[Grant]): OngoingStubbing[Future[Option[Grant]]] = {
    when(mockGrantService.validateGrant(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(grant))
  }
}
