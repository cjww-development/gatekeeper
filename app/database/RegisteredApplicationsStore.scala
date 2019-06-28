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

import com.cjwwdev.logging.Logging
import database.responses._
import database.tables.RegisteredApplications
import javax.inject.Inject
import models.RegisteredApplication
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultRegisteredApplicationsStore @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                                                   implicit val ec: ExC) extends RegisteredApplicationsStore {
  override val table = TableQuery[RegisteredApplications]
}

trait RegisteredApplicationsStore extends DatabaseRepository[RegisteredApplications] with Logging {

  private type SingleReadQuery = SqlAction[Option[RegisteredApplication], NoStream, Effect.Read]

  def insertNewApplication(app: RegisteredApplication)(implicit ec: ExC): Future[MySQLCreateResponse] = {
    val stmt = table
      .map(tbl => (tbl.name, tbl.description, tbl.homeUrl, tbl.redirectUrl, tbl.clientType, tbl.clientId, tbl.clientSecret))
      .+=(app.name, app.desc, app.homeUrl, app.redirectUrl, app.clientType, app.clientId, app.clientSecret)

    db.run(stmt) map { _ =>
      logger.info(s"[insertNewApplication] - Registered new application against ${app.name}")
      MySQLSuccessCreate
    } recover {
      case e =>
        logger.error(s"[insertNewApplication] - There was a problem registering the new application", e)
        MySQLFailedCreate
    }
  }

  def getOneApplication(query: SingleReadQuery)(implicit ec: ExC): Future[Either[MySQLReadResponse, RegisteredApplication]] = {
    db.run(query) map {
      case Some(app) =>
        logger.info(s"[getApplication] - Application ${app.name} found")
        Right(app)
      case None      =>
        logger.warn(s"[getApplication] - No matching application found")
        Left(MySQLFailedRead)
    }
  }

  def getAllApplications(implicit ec: ExC): Future[Seq[RegisteredApplication]] = {
    db.run(table.result) map { seq =>
      logger.info(s"[getAllApplications] - Found ${seq.size} applications")
      seq
    }
  }

  def removeRegisteredApplication(name: String)(implicit ec: ExC): Future[MySQLDeleteResponse] = {
    db.run(table.filter(_.name === name).delete) map {
      case 1 =>
        logger.info(s"[removeRegisteredApplication] - The registered application named $name has been removed")
        MySQLSuccessDelete
      case 0 =>
        logger.info(s"[removeRegisteredApplication] - Problem deleting application named $name; did it exist?")
        MySQLFailedDelete
    }
  }
}
