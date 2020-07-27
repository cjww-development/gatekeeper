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

import database.{IndividualUserStore, OrganisationUserStore}
import javax.inject.Inject
import models.User
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.model.Filters._
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultLoginService @Inject()(val userStore: IndividualUserStore,
                                    val orgUserStore: OrganisationUserStore) extends LoginService

trait LoginService {

  val userStore: IndividualUserStore
  val orgUserStore: OrganisationUserStore

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getUserSalt(accountId: String)(implicit ec: ExC): Future[Option[String]] = {
    for {
      indUserName <- userStore.projectValue("userName", accountId, "salt")
      indEmail <- userStore.projectValue("email", accountId, "salt")
      orgUserName <- orgUserStore.projectValue("userName", accountId, "salt")
      orgEmail <- orgUserStore.projectValue("email", accountId, "salt")
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
        equal("email", accountId)
      ),
      equal("password", password)
    )

    for {
      ind <- userStore.validateUserOn(query)
      org <- orgUserStore.validateUserOn(query)
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
}
