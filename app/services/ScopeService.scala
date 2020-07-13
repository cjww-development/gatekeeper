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

package services

import com.cjwwdev.security.SecurityConfiguration
import com.cjwwdev.security.deobfuscation.DeObfuscators
import database.{IndividualUserStore, OrganisationUserStore}
import javax.inject.Inject
import models.Scopes
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.LoggerFactory
import play.api.Configuration

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultScopeService @Inject()(val config: Configuration) extends ScopeService

trait ScopeService {

  val config: Configuration

  def getValidScopes: Scopes = {
    Scopes(
      reads = config.get[Seq[String]]("scopes.read"),
      writes = config.get[Seq[String]]("scopes.write")
    )
  }
}
