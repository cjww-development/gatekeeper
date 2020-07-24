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

import com.cjwwdev.security.SecurityConfiguration
import com.cjwwdev.security.deobfuscation.DeObfuscators
import database.{IndividualUserStore, OrganisationUserStore}
import javax.inject.Inject
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultAccountService @Inject()(val userStore: IndividualUserStore,
                                      val orgUserStore: OrganisationUserStore) extends AccountService

trait AccountService extends DeObfuscators with SecurityConfiguration {

  val userStore: IndividualUserStore
  val orgUserStore: OrganisationUserStore

  override val locale: String = "models.User"

  override val logger = LoggerFactory.getLogger(this.getClass)

  def getIndividualAccountInfo(userId: String)(implicit ec: ExC): Future[Map[String, String]] = {
    userStore.projectValue("id", userId, "userName", "email", "createdAt") map { data =>
      if(data.nonEmpty) {
        logger.info(s"[getIndividualAccountInfo] - Found user data for user $userId")
        Map(
          "userName"  -> stringDeObfuscate.decrypt(data("userName").asString().getValue).getOrElse("---"),
          "email"     -> stringDeObfuscate.decrypt(data("email").asString().getValue).getOrElse("---"),
          "createdAt" -> new DateTime(
            data("createdAt").asDateTime().getValue, DateTimeZone.UTC
          ).toString("yyyy-MM-dd")
        )
      } else {
        logger.info(s"[getIndividualAccountInfo] - Could not find data for user $userId")
        Map()
      }
    }
  }

  def getOrganisationAccountInfo(userId: String)(implicit ec: ExC): Future[Map[String, String]] = {
    orgUserStore.projectValue("id", userId, "userName", "email", "createdAt") map { data =>
      if(data.nonEmpty) {
        logger.info(s"[getOrganisationAccountInfo] - Found user data for user $userId")
        Map(
          "userName"  -> stringDeObfuscate.decrypt(data("userName").asString().getValue).getOrElse("---"),
          "email"     -> stringDeObfuscate.decrypt(data("email").asString().getValue).getOrElse("---"),
          "createdAt" -> new DateTime(
            data("createdAt").asDateTime().getValue, DateTimeZone.UTC
          ).toString("yyyy-MM-dd")
        )
      } else {
        logger.info(s"[getOrganisationAccountInfo] - Could not find data for user $userId")
        Map()
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
}
