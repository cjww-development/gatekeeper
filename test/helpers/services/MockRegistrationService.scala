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

import dev.cjww.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoSuccessCreate}
import models.{RegisteredApplication, User}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.users.RegistrationService

import scala.concurrent.{Future, ExecutionContext => ExC}

trait MockRegistrationService extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockRegistrationService: RegistrationService = mock[RegistrationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRegistrationService)
  }

  def mockCreateNewUser(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockRegistrationService.createNewUser(ArgumentMatchers.any[User]())(ArgumentMatchers.any[ExC]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockIsIdentifierInUse(inUse: Boolean): OngoingStubbing[Future[Boolean]] = {
    when(mockRegistrationService.isIdentifierInUse(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(inUse))
  }

  def mockValidateSalt(salt: String): OngoingStubbing[Future[String]] = {
    when(mockRegistrationService.validateSalt(ArgumentMatchers.any[String]())(ArgumentMatchers.any[ExC]()))
      .thenReturn(Future.successful(salt))
  }

  def mockCreateApp(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockRegistrationService.createApp(ArgumentMatchers.any[RegisteredApplication]())(ArgumentMatchers.any[ExC]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockValidateIdsAndSecrets(app: RegisteredApplication): OngoingStubbing[Future[RegisteredApplication]] = {
    when(mockRegistrationService.validateIdsAndSecrets(ArgumentMatchers.any[RegisteredApplication]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(app))
  }
}
