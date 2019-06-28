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

package global

import com.cjwwdev.logging.Logging
import javax.inject.Inject
import play.api.db.DBApi
import play.api.db.evolutions.EvolutionsComponents
import play.api.{Configuration, Environment}
import play.core.WebCommands

import scala.util.{Failure, Success, Try}

class EvolutionsApplicator @Inject()(val configuration: Configuration,
                                     val environment: Environment,
                                     val dbApi: DBApi,
                                     val webCommands: WebCommands) extends EvolutionsComponents with Logging {
  logger.info("Applying Slick MySQL Database evolutions...")
  Try(applicationEvolutions.start()) match {
    case Success(_) => logger.info("Slick MySQL Database evolutions applied")
    case Failure(e) => logger.error("There was a problem applying the Slick MySQL Database evolutions", e)
  }
}
