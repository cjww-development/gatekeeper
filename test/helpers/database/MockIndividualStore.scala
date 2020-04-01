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
import database.IndividualUserStore
import models.User
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}

trait MockIndividualStore extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockIndividualStore: IndividualUserStore = mock[IndividualUserStore]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIndividualStore)
  }

  def mockCreateIndividualUser(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockIndividualStore.createUser(ArgumentMatchers.any[User]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockIndividualValidateUserOn(user: Option[User]): OngoingStubbing[Future[Option[User]]] = {
    when(mockIndividualStore.validateUserOn(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]()))
      .thenReturn(Future.successful(user))
  }
}
