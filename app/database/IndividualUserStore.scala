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
import models.User
import models.User._
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultIndividualUserStore @Inject()(val configuration: Configuration) extends IndividualUserStore with ConnectionSettings {
  override val config: Config = configuration.underlying
}

trait IndividualUserStore extends DatabaseRepository {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("id"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("userName"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("email"), IndexOptions().background(false).unique(true))
  )

  def createUser(user: User)(implicit ec: ExC): Future[MongoCreateResponse] = {
    collection[User].insertOne(user).toFuture().map { _ =>
      logger.info(s"[createNewUser] - Created new user under userId ${user.id}")
      MongoSuccessCreate
    }recover {
      case e =>
        logger.error(s"[createNewUser] - There was a problem creating a new user under userId ${user.id}", e)
        MongoFailedCreate
    }
  }

  def validateUserOn(key: String, value: String): Future[Option[User]] = {
    collection[User]
      .find(equal(key, value))
      .first()
      .toFutureOption()
  }
}
