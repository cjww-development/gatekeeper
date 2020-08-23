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

import com.cjwwdev.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate}
import com.cjwwdev.security.SecurityConfiguration
import com.cjwwdev.security.deobfuscation.DeObfuscators
import database.{IndividualUserStore, OrganisationUserStore}
import javax.inject.Inject
import models.UserInfo
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._
import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait LinkResponse
case object LinkSuccess extends LinkResponse
case object LinkFailed extends LinkResponse
case object NoUserFound extends LinkResponse

class DefaultAccountService @Inject()(val userStore: IndividualUserStore,
                                      val orgUserStore: OrganisationUserStore) extends AccountService

trait AccountService extends DeObfuscators with SecurityConfiguration {

  val userStore: IndividualUserStore
  val orgUserStore: OrganisationUserStore

  override val locale: String = "models.User"

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def getIndividualAccountInfo(userId: String)(implicit ec: ExC): Future[Option[UserInfo]] = {
    userStore.projectValue("id", userId, "userName", "email", "createdAt", "authorisedClients") map { data =>
      if(data.nonEmpty) {
        logger.info(s"[getIndividualAccountInfo] - Found user data for user $userId")
        Some(UserInfo(
          id = data("id").asString().getValue,
          userName = stringDeObfuscate.decrypt(data("userName").asString().getValue).getOrElse("---"),
          email = stringDeObfuscate.decrypt(data("email").asString().getValue).getOrElse("---"),
          accType = "individual",
          authorisedClients = data
            .get("authorisedClients")
            .map(_.asArray().getValues.asScala.map(tag => tag.asString().getValue).toList)
            .getOrElse(List()),
          createdAt = new DateTime(
            data("createdAt").asDateTime().getValue, DateTimeZone.UTC
          )
        ))
      } else {
        logger.info(s"[getIndividualAccountInfo] - Could not find data for user $userId")
        None
      }
    }
  }

  def getOrganisationAccountInfo(userId: String)(implicit ec: ExC): Future[Option[UserInfo]] = {
    orgUserStore.projectValue("id", userId, "userName", "email", "createdAt", "authorisedClients") map { data =>
      if(data.nonEmpty) {
        logger.info(s"[getOrganisationAccountInfo] - Found user data for user $userId")
        Some(UserInfo(
          id = data("id").asString().getValue,
          userName = stringDeObfuscate.decrypt(data("userName").asString().getValue).getOrElse("---"),
          email = stringDeObfuscate.decrypt(data("email").asString().getValue).getOrElse("---"),
          accType = "organisation",
          authorisedClients = data
            .get("authorisedClients")
            .map(_.asArray().getValues.asScala.map(tag => tag.asString().getValue).toList)
            .getOrElse(List()),
          createdAt = new DateTime(
            data("createdAt").asDateTime().getValue, DateTimeZone.UTC
          )
        ))
      } else {
        logger.info(s"[getOrganisationAccountInfo] - Could not find data for user $userId")
        None
      }
    }
  }

  def determineAccountTypeFromId(id: String): Option[String] = {
    id.startsWith("user-") -> id.startsWith("org-user-") match {
      case (true, false) => Some("individual")
      case (false, true) => Some("organisation")
      case (_, _)        => None
    }
  }

  def linkAuthorisedClientTo(userId: String, appId: String)(implicit ec: ExC): Future[LinkResponse] = {
    val query = equal("id", userId)
    val update: List[String] => Bson = clients => set("authorisedClients", clients)

    def linkAppToIndUser: Future[LinkResponse] = {
      userStore.validateUserOn(query) flatMap {
        case Some(user) => userStore.updateUser(query, update(user.authorisedClients.getOrElse(List()) ++ List(appId))) map {
          case MongoSuccessUpdate =>
            logger.info(s"[linkAuthorisedClientTo] - Successfully linked $appId to user $userId")
            LinkSuccess
          case MongoFailedUpdate =>
            logger.warn(s"[linkAuthorisedClientTo] - There was a problem linking $appId to user $userId")
            LinkFailed
        }
        case None =>
          logger.warn(s"[linkAuthorisedClientTo] - Failed to link client to user; user not found")
          Future.successful(NoUserFound)
      }
    }

    def linkAppToOrgUser: Future[LinkResponse] = {
      orgUserStore.validateUserOn(query) flatMap {
        case Some(user) => orgUserStore.updateUser(query, update(user.authorisedClients.getOrElse(List()) ++ List(appId))) map {
          case MongoSuccessUpdate =>
            logger.info(s"[linkAuthorisedClientTo] - Successfully linked $appId to user $userId")
            LinkSuccess
          case MongoFailedUpdate =>
            logger.warn(s"[linkAuthorisedClientTo] - There was a problem linking $appId to user $userId")
            LinkFailed
        }
        case None =>
          logger.warn(s"[linkAuthorisedClientTo] - Failed to link client to user; user not found")
          Future.successful(NoUserFound)
      }
    }

    userId match {
      case id if id.startsWith("user-") => linkAppToIndUser
      case id if id.startsWith("org-user-") => linkAppToOrgUser
      case _ =>
        logger.error(s"[linkAuthorisedClientTo] - Invalid user Id $userId")
        Future.successful(NoUserFound)
    }
  }
}
