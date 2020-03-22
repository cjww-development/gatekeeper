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

package models

import java.util.UUID

import com.cjwwdev.security.SecurityConfiguration
import com.cjwwdev.security.obfuscation.Obfuscators
import play.api.libs.json.{JsSuccess, JsValue, Reads}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

case class RegisteredApplication(name: String,
                                 desc: String,
                                 homeUrl: String,
                                 redirectUrl: String,
                                 clientType: String,
                                 clientId: String,
                                 clientSecret: Option[String])

object RegisteredApplication extends Obfuscators with SecurityConfiguration {
  implicit val inboundReads: Reads[RegisteredApplication] = (json: JsValue) => {
    val clientType = json.\("clientType").as[String]

    JsSuccess(RegisteredApplication(
      json.\("name").as[String],
      json.\("desc").as[String],
      json.\("homeUrl").as[String],
      json.\("redirectUrl").as[String],
      clientType,
      clientId = encrypt(UUID.randomUUID().toString.replace("-", "")),
      clientSecret = clientType match {
        case "confidential" => Some(encrypt(s"${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "")))
        case "public" => None
      }
    ))
  }

  override val locale: String = this.getClass.getCanonicalName

  implicit val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[RegisteredApplication]), DEFAULT_CODEC_REGISTRY)
}
