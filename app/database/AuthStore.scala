/*
 * Copyright 2019 CJWW Development
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
import global.Logging
import javax.inject.Inject
import models.AuthCode
import models.AuthCode._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import play.api.Configuration

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultAuthStore @Inject()(implicit val ec: ExC,
                                 conf: Configuration) extends AuthStore with ConnectionSettings {
  override val config: Config = conf.underlying
}

trait AuthStore extends DatabaseRepository with Indexing with Logging {

  implicit val ec: ExC

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(
      Indexes.ascending("email"),
      IndexOptions()
        .background(false)
        .unique(true)
    )
  )

  override def createIndexes: Future[Seq[String]] = {
    Future.sequence(indexes.map(idx => {
      ensureSingleIndex[AuthCode](idx)(AuthCode.classTag, AuthCode.mongoCodec)
    })).map(_.flatten)
  }

  def saveCode(code: AuthCode): Future[MongoCreateResponse] = {
    collection[AuthCode]
      .insertOne(code)
      .toFuture()
      .map { _ =>
        logger.info(s"[saveCode] - Saved validated auth code request")
        MongoSuccessCreate
      } recover { e =>
        logger.error(s"[saveCode] - There was a problem inserting the new auth code request")
        MongoFailedCreate
      }
  }

  def getCode(code: String, state: String): Future[Boolean] = {
    collection[AuthCode]
      .find(equal("code", code))
      .toFuture()
      .map(_.head.state == state)
      .recover(_ => false)
  }
}
