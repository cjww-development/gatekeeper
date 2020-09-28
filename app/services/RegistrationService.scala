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
import database.{AppStore, IndividualUserStore, OrganisationUserStore, UserStore, UserStoreUtils}
import javax.inject.{Inject, Named}
import models.{RegisteredApplication, User}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.slf4j.LoggerFactory
import utils.StringUtils

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultRegistrationService @Inject()(@Named("individualUserStore") val individualUserStore: UserStore,
                                           @Named("organisationUserStore") val organisationUserStore: UserStore,
                                           val appStore: AppStore) extends RegistrationService

trait RegistrationService extends UserStoreUtils {

  val appStore: AppStore

  private val logger = LoggerFactory.getLogger(this.getClass)

  def createNewUser(user: User)(implicit ec: ExC): Future[MongoCreateResponse] = {
    logger.info(s"[createNewUser] - Creating new user with Id ${user.id}")
    getUserStore(user.id).createUser(user).map(logResponse(user))
  }

  def isIdentifierInUse(email: String, userName: String)(implicit ec: ExC): Future[Boolean] = {
    val query: String => Bson = accountId => or(
      equal("email", accountId),
      equal("userName", accountId)
    )

    for {
      indEmail    <- individualUserStore.findUser(query(email))
      indUserName <- individualUserStore.findUser(query(userName))
      orgEmail    <- organisationUserStore.findUser(query(email))
      orgUserName <- organisationUserStore.findUser(query(userName))
    } yield {
      (indEmail.nonEmpty || indUserName.nonEmpty) -> (orgEmail.nonEmpty || orgUserName.nonEmpty) match {
        case (false, false) => false
        case (true, false)  => true
        case (false, true)  => true
        case (true, true)   => true
      }
    }
  }

  def validateSalt(salt: String)(implicit ec: ExC): Future[String] = {
    val query = equal("salt", salt)
    individualUserStore.findUser(query) flatMap { ind =>
      organisationUserStore.findUser(query) flatMap { org =>
        if(!(ind.isEmpty && org.isEmpty)) {
          logger.info("[revalidateSalt] - Current salt is in use, regenerating salt and revalidating")
          val salt = StringUtils.salter(length = 32)
          validateSalt(salt)
        } else {
          logger.info("[revalidateSalt] - Current salt deemed ok to use")
          Future.successful(salt)
        }
      }
    }
  }

  def createApp(app: RegisteredApplication)(implicit ec: ExC): Future[MongoCreateResponse] = {
    appStore.createApp(app)
  }

  def validateIdsAndSecrets(app: RegisteredApplication)(implicit ec: ExC): Future[RegisteredApplication] = {
    def validate(clientId: String, clientSecret: Option[String])(implicit ec: ExC): Future[Boolean] = {
      for {
        appId     <- appStore.validateAppOn(equal("clientId", clientId))
        appSecret <- clientSecret.fold(Future.successful(Option.empty[RegisteredApplication])) {
          sec => appStore.validateAppOn(equal("clientSecret", sec))
        }
      } yield {
        val inUse = appId.nonEmpty || appSecret.nonEmpty
        logger.info(s"[areIdsAndSecretsInUse] - Are the selected ids and secrets in use? $inUse")
        inUse
      }
    }

    validate(app.clientId, app.clientSecret) flatMap { inUse =>
      if(inUse) validateIdsAndSecrets(app.regenerateIdsAndSecrets) else Future.successful(app)
    }
  }

  private def logResponse(user: User): PartialFunction[MongoCreateResponse, MongoCreateResponse] = {
    case resp@MongoSuccessCreate =>
      logger.info(s"[createNewUser] - Created new ${user.accType} user against Id ${user.id}")
      resp
    case resp@MongoFailedCreate =>
      logger.error(s"[createNewUser] - There was a problem creating a ${user.accType} user against Id ${user.id}")
      resp
  }
}
