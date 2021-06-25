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

import models.UserInfo
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.users.{LinkResponse, UserService}

import scala.concurrent.Future

trait MockAccountService extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockAccountService: UserService = mock[UserService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAccountService)
  }

  def mockGetIndividualAccountInfo(value: Option[UserInfo]): OngoingStubbing[Future[Option[UserInfo]]] = {
    when(mockAccountService.getUserInfo(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(value))
  }

  def mockGetOrganisationAccountInfo(value: Option[UserInfo]): OngoingStubbing[Future[Option[UserInfo]]] = {
    when(mockAccountService.getUserInfo(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(value))
  }

  def mockLinkAuthorisedClientTo(resp: LinkResponse): OngoingStubbing[Future[LinkResponse]] = {
    when(mockAccountService.linkClientToUser(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[Seq[String]]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }
}
