/*
 * Copyright 2022 CJWW Development
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
import dev.cjww.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoSuccessCreate}
import models.LoginAttempt
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultLoginAttemptStore @Inject()(val configuration: Configuration) extends LoginAttemptStore with ConnectionSettings {
  override val config: Config = configuration.underlying
  override val expiry: Long = configuration.get[Long]("login-attempt.expire-after")
}

trait LoginAttemptStore extends DatabaseRepository with CodecReg {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val expiry: Long

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("createdAt"), IndexOptions().background(false).expireAfter(expiry, TimeUnit.MILLISECONDS))
  )

  def createLoginAttempt(attempt: LoginAttempt)(implicit ec: ExC): Future[MongoCreateResponse] = {
    collection[LoginAttempt]
      .insertOne(attempt)
      .toFuture()
      .map { _ =>
        logger.info(s"[createLoginAttempt] - Created new login attempt")
        MongoSuccessCreate
      }.recover { e =>
        logger.error(s"[createLoginAttempt] - There was a problem creating the new login attempt", e)
        MongoFailedCreate
      }
  }

  def validateLoginAttempt(query: Bson): Future[Option[LoginAttempt]] = {
    collection[LoginAttempt]
      .find(query)
      .first()
      .toFutureOption()
  }
}
