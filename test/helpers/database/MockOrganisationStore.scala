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

import database.OrganisationUserStore
import dev.cjww.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoSuccessCreate, MongoUpdatedResponse}
import models.User
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.conversions.Bson
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}

trait MockOrganisationStore extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockOrganisationStore: OrganisationUserStore = mock[OrganisationUserStore]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOrganisationStore)
  }

  def mockCreateOrgUser(success: Boolean): OngoingStubbing[Future[MongoCreateResponse]] = {
    when(mockOrganisationStore.createUser(ArgumentMatchers.any[User]())(ArgumentMatchers.any[ExecutionContext]()))
      .thenReturn(Future.successful(if(success) MongoSuccessCreate else MongoFailedCreate))
  }

  def mockOrganisationValidateUserOn(user: Option[User]): OngoingStubbing[Future[Option[User]]] = {
    when(mockOrganisationStore.findUser(ArgumentMatchers.any[Bson]()))
      .thenReturn(Future.successful(user))
  }

  def mockOrganisationMultipleValidateUserOn(user: Option[User]): OngoingStubbing[Future[Option[User]]] = {
    when(mockOrganisationStore.findUser(ArgumentMatchers.any[Bson]()))
      .thenReturn(Future.successful(user))
      .thenReturn(Future.successful(user))
  }

  def mockMultipleOrganisationValidateUserOn(userOne: Option[User], userTwo: Option[User]): OngoingStubbing[Future[Option[User]]] = {
    when(mockOrganisationStore.findUser(ArgumentMatchers.any[Bson]()))
      .thenReturn(Future.successful(userOne))
      .thenReturn(Future.successful(userTwo))
  }

  def mockOrganisationProjectValue(value: Map[String, BsonValue]): OngoingStubbing[Future[Map[String, BsonValue]]] = {
    when(mockOrganisationStore.projectValue(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(value))
  }

  def mockOrganisationMultipleProjectValue(valueOne: Map[String, BsonValue], valueTwo: Map[String, BsonValue]): OngoingStubbing[Future[Map[String, BsonValue]]] = {
    when(mockOrganisationStore.projectValue(ArgumentMatchers.any[String](), ArgumentMatchers.any[String](), ArgumentMatchers.any[String]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(valueOne))
      .thenReturn(Future.successful(valueTwo))
  }

  def mockUpdateOrgUser(resp: MongoUpdatedResponse): OngoingStubbing[Future[MongoUpdatedResponse]] = {
    when(mockOrganisationStore.updateUser(ArgumentMatchers.any[Bson](), ArgumentMatchers.any[Bson]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(resp))
  }
}
