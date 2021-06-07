/*
 * Copyright 2021 CJWW Development
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

package database

import com.typesafe.config.Config
import dev.cjww.mongo.DatabaseRepository
import dev.cjww.mongo.connection.ConnectionSettings
import dev.cjww.mongo.responses._
import models.RegisteredApplication
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultAppStore @Inject()(val configuration: Configuration) extends AppStore with ConnectionSettings {
  override val config: Config = configuration.underlying
}

trait AppStore extends DatabaseRepository with CodecReg {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("clientId"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("clientSecret"), IndexOptions().background(false).unique(true))
  )

  def createApp(app: RegisteredApplication)(implicit ec: ExC): Future[MongoCreateResponse] = {
    collection[RegisteredApplication]
      .insertOne(app)
      .toFuture()
      .map { _ =>
        logger.info(s"[createApp] - Created new app under name ${app.name}")
        MongoSuccessCreate
      }.recover {
        case e =>
          logger.error(s"[createApp] - There was a problem creating a new app under name ${app.name}", e)
          MongoFailedCreate
      }
  }

  def validateAppOn(query: Bson): Future[Option[RegisteredApplication]] = {
    collection[RegisteredApplication]
      .find(query)
      .first()
      .toFutureOption()
  }

  def getAppsOwnedBy(orgUserId: String): Future[Seq[RegisteredApplication]] = {
    collection[RegisteredApplication]
      .find(equal("owner", orgUserId))
      .toFuture()
  }

  def updateApp(query: Bson, update: Bson)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    collection[RegisteredApplication]
      .updateOne(query, update)
      .toFuture()
      .map { _ =>
        logger.info(s"[updateApp] - Updated application")
        MongoSuccessUpdate
      }.recover {
        case e =>
          logger.warn(s"[updateApp] - There was a problem updating the app", e)
          MongoFailedUpdate
      }
  }

  def deleteApp(query: Bson)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    collection[RegisteredApplication]
      .deleteOne(query)
      .toFuture()
      .map { _ =>
        logger.info(s"[deleteApp] - The app was successfully deleted")
        MongoSuccessDelete
      } recover {
        case e =>
          logger.warn(s"[updateApp] - There was a problem deleting the app", e)
          MongoFailedDelete
      }
  }
}
