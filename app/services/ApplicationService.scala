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

import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoDeleteResponse}
import database.RegisteredApplicationsStore
import javax.inject.Inject
import models.RegisteredApplication
import models.RegisteredApplication._

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultApplicationService @Inject()(val registeredApplicationsStore: RegisteredApplicationsStore) extends ApplicationService

trait ApplicationService {

  val registeredApplicationsStore: RegisteredApplicationsStore

  def registerNewApplication(app: RegisteredApplication)(implicit ec: ExC): Future[MongoCreateResponse] = {
    registeredApplicationsStore.insertNewApplication(app)
  }

  def getAllApplications(implicit ec: ExC): Future[Seq[RegisteredApplication]] = {
    registeredApplicationsStore.getAllApplications
  }

  def getServiceByName(key: String, value: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    registeredApplicationsStore.getOneApplication(key, value)
  }

  def removeRegisteredApplication(name: String)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    registeredApplicationsStore.removeRegisteredApplication(name)
  }
}
