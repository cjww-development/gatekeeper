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

import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.Configuration

class DefaultScopeService @Inject()(val config: Configuration) extends ScopeService {
  override protected val validScopes: Seq[String] = config.get[Seq[String]]("well-known-config.scopes")
}

trait ScopeService {

  protected val validScopes: Seq[String]

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getValidScopes: Seq[String] = {
    validScopes
  }

  def validateScopes(scopes: String): Boolean = {
    val inboundScopes = scopes.split(",").map(_.trim)
    val validatedScopes = inboundScopes.map(validScopes.contains)
    val valid = !validatedScopes.contains(false)
    logger.info(s"[validateRequestedScopes] - Are the requested scopes valid? $valid")
    valid
  }
}
