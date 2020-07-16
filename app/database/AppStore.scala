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

package database

import com.cjwwdev.mongo.DatabaseRepository
import com.cjwwdev.mongo.connection.ConnectionSettings
import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoSuccessCreate}
import com.typesafe.config.Config
import javax.inject.Inject
import models.RegisteredApplication
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

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

  def validateAppOn(key: String, value: String): Future[Option[RegisteredApplication]] = {
    collection[RegisteredApplication]
      .find(equal(key, value))
      .first()
      .toFutureOption()
  }
}
