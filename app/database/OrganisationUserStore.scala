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

package database

import com.typesafe.config.Config
import dev.cjww.mongo.connection.ConnectionSettings
import play.api.Configuration

import javax.inject.Inject

class OrganisationUserStore @Inject()(val configuration: Configuration) extends UserStore with ConnectionSettings {
  override val config: Config = configuration.underlying
}
