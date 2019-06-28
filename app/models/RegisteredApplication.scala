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

package models

import java.util.UUID

import com.cjwwdev.implicits.ImplicitDataSecurity._
import com.cjwwdev.implicits.ImplicitJsValues._
import com.cjwwdev.security.deobfuscation.{DeObfuscation, DeObfuscator}
import com.cjwwdev.security.obfuscation.Obfuscation._
import play.api.libs.json._

case class RegisteredApplication(name: String,
                                 desc: String,
                                 homeUrl: String,
                                 redirectUrl: String,
                                 clientType: String,
                                 clientId: String,
                                 clientSecret: Option[String]) {
  def toMap: Map[String, String] = Map(
    "name"        -> name,
    "desc"        -> desc,
    "homeUrl"     -> homeUrl,
    "redirectUrl" -> redirectUrl,
    "clientType"  -> clientType,
    "clientId"    -> clientId,
    clientSecret.fold("" -> "")(sec => "clientSecret" -> sec)
  )
}

object RegisteredApplicationCompanion {
  implicit val inboundReads = new Reads[RegisteredApplication] {
    override def reads(json: JsValue): JsResult[RegisteredApplication] = {
      val clientType = json.get[String]("clientType")

      JsSuccess(RegisteredApplication(
        json.get[String]("name"),
        json.get[String]("desc"),
        json.get[String]("homeUrl"),
        json.get[String]("redirectUrl"),
        clientType,
        clientId     = UUID.randomUUID().toString.replace("-", "").encrypt,
        clientSecret = clientType match {
          case "confidential" => Some(s"${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "").encrypt)
          case "public"       => None
        }
      ))
    }
  }

  implicit val inboundDeObfuscator: DeObfuscator[RegisteredApplication] = (value: String) => {
    DeObfuscation.deObfuscate[RegisteredApplication](value)(inboundReads, implicitly)
  }

  implicit val writes = Json.writes[RegisteredApplication]
}
