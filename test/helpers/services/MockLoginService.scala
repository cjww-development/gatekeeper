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

import models.User
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.users.LoginService

import java.util.UUID
import scala.concurrent.Future

trait MockLoginService extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockLoginService: LoginService = mock[LoginService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockLoginService)
  }

  def mockGetUserSalt(salt: Option[String]): OngoingStubbing[Future[Option[String]]] = {
    when(mockLoginService.getUserSalt(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(salt))
  }

  def mockValidateUser(user: Option[User]): OngoingStubbing[Future[Option[User]]] = {
    when(mockLoginService.validateUser(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(user))
  }

  def mockSaveLoginAttempt(success: Boolean): OngoingStubbing[Future[Option[String]]] = {
    when(mockLoginService.saveLoginAttempt(ArgumentMatchers.any[String](), ArgumentMatchers.any[Boolean]())(ArgumentMatchers.any()))
      .thenReturn(if(success) Future.successful(Some(s"att-${UUID.randomUUID().toString}")) else Future.successful(None))
  }

  def mockLookupLoginAttempt(userId: Option[String]): OngoingStubbing[Future[Option[String]]] = {
    when(mockLoginService.lookupLoginAttempt(ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(userId))
  }
}
