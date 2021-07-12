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

package helpers

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

trait IntegrationApp extends GuiceOneServerPerSuite {
  self: PlaySpec =>

  val appConfig: Map[String, Any] = Map(
    "scopes.read"                                             -> Seq("username"),
    "scopes.write"                                            -> Seq(),
    "database.IndividualUserStore.database"                   -> "gatekeeper-it",
    "database.OrganisationUserStore.database"                 -> "gatekeeper-it",
    "database.DefaultAppStore.database"                       -> "gatekeeper-it",
    "database.DefaultGrantStore.database"                     -> "gatekeeper-it",
    "database.DefaultLoginAttemptStore.database"              -> "gatekeeper-it",
    "database.DefaultTokenRecordStore.database"               -> "gatekeeper-it",
    "database.DefaultEmailVerificationStore.database"         -> "gatekeeper-it",
    "play.http.router"                                        -> "testing.Routes"
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(appConfig)
    .build()

  lazy val testAppUrl = s"http://localhost:$port/gatekeeper"
  lazy val testAppPrivateUrl = s"http://localhost:$port/private"
}
