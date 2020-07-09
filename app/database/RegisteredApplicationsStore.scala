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
import models.RegisteredApplication
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import play.api.Configuration

import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.reflect.ClassTag

class DefaultRegisteredApplicationsStore @Inject()(implicit val ec: ExC,
                                                   conf: Configuration) extends RegisteredApplicationsStore with ConnectionSettings {
  override val config: Config = conf.underlying
}

trait RegisteredApplicationsStore extends DatabaseRepository with Indexing with Logging {

  implicit val ec: ExC

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(
      Indexes.ascending("name"),
      IndexOptions()
        .background(false)
        .unique(true)
    ),
    IndexModel(
      Indexes.ascending("clientId"),
      IndexOptions()
        .background(false)
        .unique(true)
    ),
    IndexModel(
      Indexes.ascending("clientSecret"),
      IndexOptions()
        .background(false)
        .unique(true)
    )
  )

  override def createIndexes: Future[Seq[String]] = {
    Future.sequence(indexes.map(idx => {
      ensureSingleIndex[RegisteredApplication](idx)(RegisteredApplication.classTag, RegisteredApplication.mongoCodec)
    })).map(_.flatten)
  }

  def insertNewApplication(app: RegisteredApplication)
                          (implicit codec: CodecRegistry, ct: ClassTag[RegisteredApplication]): Future[MongoCreateResponse] = {
    collection[RegisteredApplication]
      .insertOne(app)
      .toFuture()
      .map { _ =>
        logger.info(s"[insertNewApplication] - Registered new application against ${app.name}")
        MongoSuccessCreate
      } recover { e =>
        logger.error(s"[insertNewApplication] - There was a problem inserting the app ${app.name}")
        MongoFailedCreate
      }
  }

  def getOneApplication(key: String, value: String)
                       (implicit codec: CodecRegistry, ct: ClassTag[RegisteredApplication]): Future[Option[RegisteredApplication]] = {
    collection[RegisteredApplication]
      .find(equal(key, value))
      .toFuture()
      .map { seq =>
        val app = seq.headOption
        val errorLog: () => () = () => logger.warn(s"[getOneApplication] - No matching application found ")
        val foundLog: RegisteredApplication => () = app => logger.info(s"[getOneApplication] - Application ${app.name} found")
        app.fold[()](errorLog)(foundLog)
        app
      } recover { e =>
        logger.error(s"[getOneApplication] - There was a problem finding an application against ${key}")
        None
      }
  }

  def getAllApplications(implicit codec: CodecRegistry, ct: ClassTag[RegisteredApplication]): Future[Seq[RegisteredApplication]] = {
    collection[RegisteredApplication]
      .find()
      .toFuture()
      .map { seq =>
        logger.info(s"[getAllApplications] - Found ${seq.size} applications")
        seq
      } recover { e =>
        logger.error(s"[getAllApplications] - There was a problem getting all applications", e)
        Seq()
      }
  }

  def removeRegisteredApplication(name: String)
                                 (implicit codec: CodecRegistry, ct: ClassTag[RegisteredApplication]): Future[MongoDeleteResponse] = {
    collection[RegisteredApplication]
      .deleteOne(equal("name", name))
      .toFuture()
      .map {
        _.getDeletedCount match {
          case 1 =>
            logger.info(s"[removeRegisteredApplication] - The registered application named $name has been removed")
            MongoSuccessDelete
          case _ =>
            logger.warn(s"[removeRegisteredApplication] - Problem deleting application named $name; did it exist?")
            MongoFailedDelete
        }
      } recover { e =>
        logger.info(s"[removeRegisteredApplication] - Problem deleting application named $name; did it exist?")
        MongoFailedDelete
      }
  }
}
