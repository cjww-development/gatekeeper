/*
 * Copyright 2022 CJWW Development
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

package services.oauth2

import com.typesafe.config.Config
import models.Scope
import org.slf4j.LoggerFactory
import play.api.Configuration

import javax.inject.Inject

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

  def getScopeDetails(strScopes: Seq[String]): Seq[Scope] = {
    approvedScopes.filter(scp => strScopes.contains(scp.name))
  }

  def validateScopes(scopes: String): Boolean = {
    val inboundScopes = scopes.split(" ").map(_.trim)
    val validatedScopes = inboundScopes.map(str => approvedScopes.exists(_.name == str))
    val valid = !validatedScopes.contains(false)
    logger.info(s"[validateScopes] - Are the requested scopes valid? $valid")
    valid
  }

  def validateScopes(scopes: String, appScopes: Seq[String]): Boolean = {
    val inboundScopes = scopes.split(" ").map(_.trim)
    val validatedScopes = inboundScopes.map(str => appScopes.contains(str))
    val valid = !validatedScopes.contains(false)
    logger.info(s"[validateScopes] - Are the requested scopes valid? $valid")
    valid
  }
}
