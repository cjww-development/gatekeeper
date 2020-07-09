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
import com.cjwwdev.mongo.responses._
import com.typesafe.config.Config
import global.Logging
import javax.inject.Inject
import models.User
import models.User._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import play.api.Configuration

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultUserStore @Inject()(implicit val ec: ExC,
                                 conf: Configuration) extends UserStore with ConnectionSettings {
  override val config: Config = conf.underlying
}

trait UserStore extends DatabaseRepository with Indexing with Logging {

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
      ensureSingleIndex[User](idx)(User.classTag, User.mongoCodec)
    })).map(_.flatten)
  }

  def createUser(user: User): Future[MongoCreateResponse] = {
    collection[User]
      .insertOne(user)
      .toFuture()
      .map { _ =>
        logger.info(s"[insertNewApplication] - Registered new user")
        MongoSuccessCreate
      } recover { e =>
        logger.error(s"[insertNewApplication] - There was a problem inserting the new users")
        MongoFailedCreate
      }
  }

  def getUser(key: String, value: String): Future[Option[User]] = {
    collection[User]
      .find(equal(key, value))
      .toFuture()
      .map { seq =>
        val app = seq.headOption
        val errorLog: () => () = () => logger.warn(s"[getUser] - No matching user found ")
        val foundLog: () => () = () => logger.info(s"[getUser] - User found")
        app.fold[()](errorLog)(_ => foundLog())
        app
      } recover { e =>
        logger.error(s"[getOneApplication] - There was a problem finding an application against ${key}")
        None
      }
  }
}
