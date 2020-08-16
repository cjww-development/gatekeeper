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

package orchestrators

import javax.inject.Inject
import models.RegisteredApplication
import org.slf4j.LoggerFactory
import services.ClientService
import com.cjwwdev.security.Implicits._
import com.cjwwdev.security.deobfuscation.DeObfuscators
import com.cjwwdev.security.obfuscation.Obfuscators

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultClientOrchestrator @Inject()(val clientService: ClientService) extends ClientOrchestrator {
  override val locale: String = ""
}

trait ClientOrchestrator extends Obfuscators with DeObfuscators {

  protected val clientService: ClientService

  override val logger = LoggerFactory.getLogger(this.getClass)

  def getRegisteredApp(orgUserId: String, appId: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    clientService.getRegisteredApp(orgUserId, appId).map {
      _.map(app => app.copy(
        clientId = stringDeObfuscate.decrypt(app.clientId).getOrElse(throw new Exception(s"Could not decrypt clientId for app ${app.appId}")),
        clientSecret = app.clientSecret.map(sec => stringDeObfuscate.decrypt(sec).getOrElse(throw new Exception(s"Could not decrypt clientSecret for app ${app.appId}")))
      ))
    }
  }

  def getRegisteredApps(orgUserId: String, groupedBy: Int)(implicit ec: ExC): Future[Seq[Seq[RegisteredApplication]]] = {
    clientService.getRegisteredAppsFor(orgUserId) map {
      _.map(app => app.copy(
        clientId = stringDeObfuscate.decrypt(app.clientId).getOrElse(throw new Exception(s"Could not decrypt clientId for app ${app.appId}")),
        clientSecret = app.clientSecret.map(sec => stringDeObfuscate.decrypt(sec).getOrElse(throw new Exception(s"Could not decrypt clientSecret for app ${app.appId}")))
      ))
      .grouped(groupedBy)
      .toSeq
    }
  }
}
