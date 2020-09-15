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

import com.typesafe.config.Config
import javax.inject.Inject
import models.Scope
import org.slf4j.LoggerFactory
import play.api.Configuration

class DefaultScopeService @Inject()(val config: Configuration) extends ScopeService {
  override protected val approvedScopes: Seq[Scope] = config.get[Seq[Config]]("valid-scopes").map { conf =>
    Scope(
      name = conf.getString("name"),
      readableName = conf.getString("readable-name"),
      desc = conf.getString("desc")
    )
  }
}

trait ScopeService {

  protected val approvedScopes: Seq[Scope]

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getValidScopes: Seq[Scope] = {
    approvedScopes
  }

  def validateScopes(scopes: String): Boolean = {
    val inboundScopes = scopes.split(",").map(_.trim)
    val validatedScopes = inboundScopes.map(str => approvedScopes.exists(_.name == str))
    val valid = !validatedScopes.contains(false)
    logger.info(s"[validateRequestedScopes] - Are the requested scopes valid? $valid")
    valid
  }
}
