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

package models

import org.bson.codecs.configuration.CodecProvider
import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.Macros

import java.util.Date

case class Profile(name: Option[String],
                   familyName: Option[String],
                   givenName: Option[String],
                   middleName: Option[String],
                   nickname: Option[String],
                   profile: Option[String],
                   picture: Option[String],
                   website: Option[String],
                   gender: Option[String],
                   birthDate: Option[Date],
                   zoneinfo: Option[String],
                   locale: Option[String],
                   updatedAt: Option[DateTime])

object Profile {
  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[Profile]()
}
