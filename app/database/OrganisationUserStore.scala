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
import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoFailedUpdate, MongoSuccessCreate, MongoSuccessUpdate, MongoUpdatedResponse}
import com.typesafe.config.Config
import javax.inject.Inject
import models.User
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections.{excludeId, fields, include}
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultOrganisationUserStore @Inject()(val configuration: Configuration) extends OrganisationUserStore with ConnectionSettings {
  override val config: Config = configuration.underlying
}

trait OrganisationUserStore extends DatabaseRepository with CodecReg {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("id"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("userName"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("email"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("salt"), IndexOptions().background(false).unique(true)),
  )

  def createUser(user: User)(implicit ec: ExC): Future[MongoCreateResponse] = {
    collection[User]
      .insertOne(user)
      .toFuture()
      .map { _ =>
        logger.info(s"[createNewUser] - Created new user under userId ${user.id}")
        MongoSuccessCreate
      } recover {
        case e =>
          logger.error(s"[createNewUser] - There was a problem creating a new user under userId ${user.id}", e)
          MongoFailedCreate
      }
  }

  def validateUserOn(query: Bson): Future[Option[User]] = {
    collection[User]
      .find(query)
      .first()
      .toFutureOption()
  }

  def projectValue(key: String, value: String, projections: String*)(implicit ec: ExC): Future[Map[String, BsonValue]] = {
    def buildMap(doc: Option[Document]): Map[String, BsonValue] = {
      doc.fold[Map[String, BsonValue]](Map())(_.toMap)
    }

    val inclusions = fields((projections
      .map(str => include(str)) ++ Seq(include("id"), excludeId())):_*)

    collection[Document]
      .find(equal(key, value))
      .projection(inclusions)
      .first()
      .toFutureOption()
      .map(buildMap)
      .recover(_ => Map())
  }

  def updateUser(query: Bson, update: Bson)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    collection[User]
      .updateOne(query, update)
      .toFuture()
      .map { _ =>
        logger.info(s"[updateUser] - Updated org user information")
        MongoSuccessUpdate
      }.recover {
        case e =>
          logger.warn(s"[updateUser] - There was a problem updating the org user", e)
          MongoFailedUpdate
      }
  }
}
