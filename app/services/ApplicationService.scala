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

package services

import database.RegisteredApplicationsStore
import database.responses.{MySQLCreateResponse, MySQLDeleteResponse, MySQLFailedRead, MySQLReadResponse}
import javax.inject.Inject
import models.RegisteredApplication
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultApplicationService @Inject()(val registeredApplicationsStore: RegisteredApplicationsStore) extends ApplicationService

trait ApplicationService {

  val registeredApplicationsStore: RegisteredApplicationsStore

  def registerNewApplication(app: RegisteredApplication)(implicit ec: ExC): Future[MySQLCreateResponse] = {
    registeredApplicationsStore.insertNewApplication(app)
  }

  def getAllApplications(implicit ec: ExC): Future[Either[MySQLReadResponse, Seq[RegisteredApplication]]] = {
    registeredApplicationsStore.getAllApplications map {
      list => if(list.isEmpty) Left(MySQLFailedRead) else Right(list)
    }
  }

  def getServiceByName(value: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    val query = registeredApplicationsStore.table.filter(_.name === value).result.headOption
    registeredApplicationsStore.getOneApplication(query) map {
      _.fold(_ => None, Some(_))
    }
  }

  def removeRegisteredApplication(name: String)(implicit ec: ExC): Future[MySQLDeleteResponse] = {
    registeredApplicationsStore.removeRegisteredApplication(name)
  }
}
