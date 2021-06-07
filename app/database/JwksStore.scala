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
import dev.cjww.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoSuccessCreate}
import models.JwksContainer
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultJwksStore @Inject()(val configuration: Configuration) extends JwksStore with ConnectionSettings {
  override val config: Config = configuration.underlying
}

trait JwksStore extends DatabaseRepository with CodecReg {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("kid"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("jwk"), IndexOptions().background(false).unique(false))
  )

  def createJwks(jwks: JwksContainer)(implicit ec: ExC): Future[MongoCreateResponse] = {
    collection[JwksContainer]
      .insertOne(jwks)
      .toFuture()
      .map { _ =>
        logger.info(s"[createJwks] - Created new jwk under kid ${jwks.kid}")
        MongoSuccessCreate
      }.recover {
        case e =>
          logger.error(s"[createJwks] - There was a problem creating a new jwk under kid ${jwks.kid}", e)
          MongoFailedCreate
      }
  }

  def getAllJwks: Future[Seq[JwksContainer]] = {
    collection[JwksContainer]
      .find()
      .toFuture()
  }
}
