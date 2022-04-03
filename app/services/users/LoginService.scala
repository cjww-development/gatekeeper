/*
 * Copyright 2022 CJWW Development
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

package services.users

import database.{LoginAttemptStore, UserStore, UserStoreUtils}
import dev.cjww.mongo.responses.{MongoFailedCreate, MongoSuccessCreate}
import models.{LoginAttempt, User}
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.model.Filters._
import org.slf4j.LoggerFactory

import javax.inject.{Inject, Named}
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultLoginService @Inject()(@Named("individualUserStore") val individualUserStore: UserStore,
                                    @Named("organisationUserStore") val organisationUserStore: UserStore,
                                    val loginAttemptStore: LoginAttemptStore) extends LoginService

trait LoginService extends UserStoreUtils {

  val loginAttemptStore: LoginAttemptStore

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getUserSalt(accountId: String)(implicit ec: ExC): Future[Option[String]] = {
    val emailKey = "digitalContact.email.address"
    for {
      indUserName <- individualUserStore.projectValue("userName", accountId, "salt")
      indEmail <- individualUserStore.projectValue(emailKey, accountId, "salt")
      orgUserName <- organisationUserStore.projectValue("userName", accountId, "salt")
      orgEmail <- organisationUserStore.projectValue(emailKey, accountId, "salt")
    } yield {
      (indUserName.nonEmpty || indEmail.nonEmpty) -> (orgUserName.nonEmpty || orgEmail.nonEmpty) match {
        case (true, false)  =>
          logger.info("[getUserSalt] - Found user in the individual user store")
          matchUsers(indUserName, indEmail, "individual")
        case (false, true)  =>
          logger.info("[getUserSalt] - Found user in the organisation user store")
          matchUsers(orgUserName, orgEmail, "organisation")
        case (false, false) =>
          logger.warn(s"[getUserSalt] - No user found")
          None
        case (true, true)   =>
          logger.error(s"[getUserSalt] - User has been found in both pools")
          throw new Exception("[getUserSalt] - User name is in both user stores")
      }
    }
  }

  def validateUser(accountId: String, password: String)(implicit ec: ExC): Future[Option[User]] = {
    val query = and(
      or(
        equal("userName", accountId),
        equal("digitalContact.email.address", accountId)
      ),
      equal("password", password)
    )

    for {
      ind <- individualUserStore.findUser(query)
      org <- organisationUserStore.findUser(query)
    } yield {
      (ind.nonEmpty, org.nonEmpty) match {
        case (true, false)  =>
          logger.info(s"[validateUser] - Found matching individual user ${ind.get.id}")
          ind
        case (false, true)  =>
          logger.info(s"[validateUser] - Found matching individual user ${org.get.id}")
          org
        case (false, false) =>
          logger.warn(s"[validateUser] - No matching user found")
          None
        case (true, true)   =>
          logger.error(s"[getUserSalt] - User has been found in both pools")
          throw new Exception("[validateUser] - User name is in both user stores")
      }
    }
  }

  private def matchUsers(userOne: Map[String, BsonValue], userTwo: Map[String, BsonValue], accType: String): Option[String] = {
    userOne.nonEmpty -> userTwo.nonEmpty match {
      case (true, false) =>
        val id = userOne("id").asString().getValue
        val salt = userOne("salt").asString().getValue
        logger.info(s"[getUserSalt] - Found $accType user salt for user $id")
        Some(salt)
      case (false, true) =>
        val id = userTwo("id").asString().getValue
        val salt = userTwo("salt").asString().getValue
        logger.info(s"[getUserSalt] - Found $accType user salt for user $id")
        Some(salt)
    }
  }

  def saveLoginAttempt(userId: String, successfulAttempt: Boolean)(implicit ec: ExC): Future[Option[String]] = {
    val loginAttempt = LoginAttempt(userId, successfulAttempt)
    loginAttemptStore.createLoginAttempt(loginAttempt) map {
      case MongoSuccessCreate =>
        logger.info(
          s"[saveLoginAttempt] - Saved ${if(successfulAttempt) "a successful" else "an unsuccessful"} login attempt for user $userId under ${loginAttempt.id}"
        )
        Some(loginAttempt.id)
      case MongoFailedCreate =>
        logger.warn(s"[saveLoginAttempt] - Failed to save login attempt for user $userId")
        None
    }
  }

  def lookupLoginAttempt(attemptId: String)(implicit ec: ExC): Future[Option[String]] = {
    val query = equal("id", attemptId)
    loginAttemptStore.validateLoginAttempt(query) map { res =>
      if(res.isDefined) {
        logger.info(s"[lookupLoginAttempt] - Found login attempt matching $attemptId")
      } else {
        logger.warn(s"[lookupLoginAttempt] - Could not find login attempt matching $attemptId")
      }
      res.map(_.userId)
    }
  }
}
