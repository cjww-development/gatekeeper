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

package services

import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoSuccessCreate}
import database.{IndividualUserStore, OrganisationUserStore}
import javax.inject.Inject
import models.User
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultRegistrationService @Inject()(val userStore: IndividualUserStore,
                                           val orgUserStore: OrganisationUserStore) extends RegistrationService

trait RegistrationService {

  val userStore: IndividualUserStore
  val orgUserStore: OrganisationUserStore

  private val logger = LoggerFactory.getLogger(this.getClass)

  def createNewUser(user: User)(implicit ec: ExC): Future[MongoCreateResponse] = {
    logger.info(s"[createNewUser] - Creating new user with Id ${user.id}")
    user.accType.toUpperCase match {
      case "INDIVIDUAL"   => userStore.createUser(user).map(logResponse(user))
      case "ORGANISATION" => orgUserStore.createUser(user).map(logResponse(user))
    }
  }

  def validateEmail(email: String)(implicit ec: ExC): Future[Boolean] = {
    for {
      ind <- userStore.validateUserOn("email", email)
      org <- orgUserStore.validateUserOn("email", email)
    } yield !(ind.isEmpty && org.isEmpty)
  }

  def validateUsername(userName: String)(implicit ec: ExC): Future[Boolean] = {
    for {
      ind <- userStore.validateUserOn("userName", userName)
      org <- orgUserStore.validateUserOn("userName", userName)
    } yield !(ind.isEmpty && org.isEmpty)
  }

  private def logResponse(user: User): PartialFunction[MongoCreateResponse, MongoCreateResponse] = {
    case resp@MongoSuccessCreate =>
      logger.info(s"[createNewUser] - Created new ${user.accType.toLowerCase} user against Id ${user.id}")
      resp
    case resp@MongoFailedCreate =>
      logger.error(s"[createNewUser] - There was a problem creating a ${user.accType.toLowerCase} user against Id ${user.id}")
      resp
  }
}
