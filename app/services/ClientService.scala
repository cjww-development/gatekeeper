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

import database.AppStore
import javax.inject.Inject
import models.RegisteredApplication
import org.slf4j.LoggerFactory
import org.mongodb.scala.model.Filters.{and, equal}

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultClientService @Inject()(val appStore: AppStore) extends ClientService

trait ClientService {

  val appStore: AppStore

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getRegisteredApp(orgUserId: String, appId: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    val query = and(
      equal("owner", orgUserId),
      equal("appId", appId)
    )

    appStore.validateAppOn(query) map { app =>
      if(app.isDefined) {
        logger.info(s"[getRegisteredApp] - Found app $appId belonging to ${orgUserId}")
      } else {
        logger.warn(s"[getRegisteredApp] - No app found app $appId belonging to ${orgUserId}")
      }
      app
    }
  }

  def getRegisteredAppsFor(orgUserId: String)(implicit ec: ExC): Future[Seq[RegisteredApplication]] = {
    appStore.getAppsOwnedBy(orgUserId) map { apps =>
      if(apps.nonEmpty) {
        logger.info(s"[getRegisteredAppsFor] - Found ${apps.length} apps ${orgUserId}")
      } else {
        logger.warn(s"[getRegisteredAppsFor] - No apps found belonging to ${orgUserId}")
      }
      apps
    }
  }
}
