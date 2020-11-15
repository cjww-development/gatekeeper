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

import java.util.concurrent.TimeUnit

import com.cjwwdev.mongo.DatabaseRepository
import com.cjwwdev.mongo.connection.ConnectionSettings
import com.cjwwdev.mongo.responses._
import com.typesafe.config.Config
import javax.inject.Inject
import models.Verification
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.reflect.ClassTag

class DefaultVerificationStore @Inject()(val configuration: Configuration) extends VerificationStore with ConnectionSettings {
  override val config: Config = configuration.underlying
  override val expiry: Long = configuration.get[Long]("verifications.expire-after")
}

trait VerificationStore extends DatabaseRepository with CodecReg {

  val expiry: Long

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("verificationId"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("userId"), IndexOptions().background(false).unique(false)),
    IndexModel(Indexes.ascending("contactType"), IndexOptions().background(false).unique(false)),
    IndexModel(Indexes.ascending("contact"), IndexOptions().background(false).unique(false)),
    IndexModel(Indexes.ascending("accType"), IndexOptions().background(false).unique(false)),
    IndexModel(Indexes.ascending("createdAt"), IndexOptions().background(false).expireAfter(expiry, TimeUnit.HOURS))
  )

  private def verificationStore(implicit ct: ClassTag[Verification], codec: CodecRegistry) = collection[Verification](ct, codec)

  def createVerificationRecord(record: Verification)(implicit ec: ExC): Future[MongoCreateResponse] = {
    verificationStore
      .insertOne(record)
      .toFuture()
      .map { _ =>
        logger.info(s"[createVerificationRecord] - Created new contact verification record")
        MongoSuccessCreate
      }.recover { e =>
        logger.error(s"[createVerificationRecord] - There was a problem creating the new contact verification record", e)
        MongoFailedCreate
      }
  }

  def validateVerificationRecord(query: Bson)(implicit ec: ExC): Future[Option[Verification]] = {
    verificationStore
      .find(query)
      .first()
      .toFutureOption()
  }

  def deleteVerificationRecord(query: Bson)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    verificationStore
      .deleteOne(query)
      .toFuture()
      .map { x =>
        logger.info(s"[deleteVerificationRecord] - Deleted verification record")
        MongoSuccessDelete
      }.recover { e =>
        logger.error(s"[deleteVerificationRecord] - There was a problem deleting the verification record", e)
        MongoFailedDelete
      }
  }
}
