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
import models.TokenRecord
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.reflect.ClassTag

class DefaultTokenRecordStore @Inject()(val configuration: Configuration) extends TokenRecordStore with ConnectionSettings {
  override val config: Config = configuration.underlying
  override val expiry: Long = configuration.get[Long]("jwt.expiry")
}

trait TokenRecordStore extends DatabaseRepository with CodecReg {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val expiry: Long

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("tokenSetId"), IndexOptions().background(false)),
    IndexModel(Indexes.ascending("userId"), IndexOptions().background(false)),
    IndexModel(Indexes.ascending("appId"), IndexOptions().background(false)),
    IndexModel(Indexes.ascending("accessTokenId"), IndexOptions().background(false)),
    IndexModel(Indexes.ascending("idTokenId"), IndexOptions().background(false)),
    IndexModel(Indexes.ascending("refreshTokenId"), IndexOptions().background(false)),
    IndexModel(Indexes.ascending("issuedAt"), IndexOptions().background(false).expireAfter(expiry, TimeUnit.SECONDS))
  )

  private def tokenRecordBasedCollection(implicit ct: ClassTag[TokenRecord], codec: CodecRegistry) = collection[TokenRecord](ct, codec)

  def createTokenRecord(record: TokenRecord)(implicit ec: ExC): Future[MongoCreateResponse] = {
    tokenRecordBasedCollection
      .insertOne(record)
      .toFuture()
      .map { _ =>
        logger.info(s"[createTokenRecord] - Created new token record")
        MongoSuccessCreate
      }.recover { e =>
        logger.error(s"[createTokenRecord] - There was a problem creating the new token record", e)
        MongoFailedCreate
      }
  }

  def validateTokenRecord(query: Bson): Future[Option[TokenRecord]] = {
    tokenRecordBasedCollection
      .find(query)
      .first()
      .toFutureOption()
  }

  def getActiveRecords(query: Bson): Future[Seq[TokenRecord]] = {
    tokenRecordBasedCollection
      .find(query)
      .toFuture()
  }

  def updateTokenRecord(query: Bson, update: Bson)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    tokenRecordBasedCollection
      .updateOne(query, update)
      .toFuture()
      .map { _ =>
        logger.info(s"[updateTokenRecord] - Updated token record")
        MongoSuccessUpdate
      } recover {
        case e =>
          logger.error(s"[updateTokenRecord] - There was a problem updating the token record", e)
          MongoFailedUpdate
      }
  }

  def deleteTokenRecord(query: Bson)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    tokenRecordBasedCollection
      .deleteOne(query)
      .toFuture()
      .map { _ =>
        logger.info(s"[deleteTokenRecord] - Deleted token record")
        MongoSuccessDelete
      }.recover { e =>
        logger.error(s"[deleteTokenRecord] - There was a problem deleting the token record", e)
        MongoFailedDelete
      }
  }
}
