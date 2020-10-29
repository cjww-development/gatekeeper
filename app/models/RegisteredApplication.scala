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

import com.cjwwdev.security.obfuscation.Obfuscators
import com.cjwwdev.security.Implicits._
import org.bson.codecs.configuration.CodecProvider
import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.Macros
import play.api.libs.json._

import scala.collection.Seq

case class RegisteredApplication(appId: String,
                                 owner: String,
                                 name: String,
                                 desc: String,
                                 homeUrl: String,
                                 redirectUrl: String,
                                 clientType: String,
                                 clientId: String,
                                 clientSecret: Option[String],
                                 oauth2Flows: Seq[String],
                                 oauth2Scopes: Seq[String],
                                 idTokenExpiry: Long,
                                 accessTokenExpiry: Long,
                                 refreshTokenExpiry: Long,
                                 createdAt: DateTime)

object RegisteredApplication extends Obfuscators with TimeFormat {
  override val locale: String = this.getClass.getCanonicalName

  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[RegisteredApplication]()

  def generateIds(iterations: Int): String = {
    (0 to iterations)
      .map(_ => UUID.randomUUID().toString.replace("-", ""))
      .mkString
      .encrypt
  }

  implicit class RegisteredApplicationOps(app: RegisteredApplication) {
    def regenerateIdsAndSecrets: RegisteredApplication = {
      app.copy(
        clientId = generateIds(iterations = 0),
        clientSecret = app.clientType match {
          case "confidential" => Some(generateIds(iterations = 1))
          case "public"       => None
        }
      )
    }
  }

  def apply(owner: String, name: String, desc: String, homeUrl: String, redirectUrl: String, clientType: String): RegisteredApplication = {
    new RegisteredApplication(
      s"appId-${UUID.randomUUID().toString}",
      owner,
      name,
      desc,
      homeUrl,
      redirectUrl,
      clientType,
      clientId = generateIds(iterations = 0),
      clientSecret = clientType match {
        case "confidential" => Some(generateIds(iterations = 1))
        case "public"       => None
      },
      oauth2Flows = Seq(),
      oauth2Scopes = Seq(),
      idTokenExpiry = 3600000L,
      accessTokenExpiry = 3600000L,
      refreshTokenExpiry = 2592000000L,
      DateTime.now()
    )
  }

  def unapply(arg: RegisteredApplication): Option[(String, String, String, String, String)] = {
    Some(arg.name, arg.desc, arg.homeUrl, arg.redirectUrl, arg.clientType)
  }

  implicit val inboundReads: Reads[RegisteredApplication] = (json: JsValue) => {
    val problemFields: Map[String, JsResult[_]] = Map(
      "clientType" -> json.\("clientType").validate[String](ClientTypes.reads)
    )

    val errors = problemFields.filter({ case (_, v) => v.isError })

    if(errors.nonEmpty) {
      type JsErr = Seq[(JsPath, Seq[JsonValidationError])]
      JsError(
        errors
          .collect({ case (_, JsError(e)) => e })
          .foldLeft[JsErr](Seq())((a, b) => JsError.merge(a, b))
      )
    } else {
      val clientType: String = problemFields("clientType").get.asInstanceOf[String]

      JsSuccess(RegisteredApplication(
        appId = json.\("appId").as[String],
        owner = json.\("owner").as[String],
        name = json.\("name").as[String],
        desc = json.\("desc").as[String],
        homeUrl = json.\("homeUrl").as[String],
        redirectUrl = json.\("redirectUrl").as[String],
        clientType,
        clientId = generateIds(iterations = 1),
        clientSecret = clientType match {
          case "confidential" => Some(generateIds(iterations = 2))
          case "public"       => None
        },
        oauth2Flows = json.\("oauth2Flows").as[Seq[String]],
        oauth2Scopes = json.\("oauth2Scopes").as[Seq[String]],
        idTokenExpiry = json.\("idTokenExpiry").as[Long],
        accessTokenExpiry = json.\("accessTokenExpiry").as[Long],
        refreshTokenExpiry = json.\("refreshTokenExpiry").as[Long],
        createdAt = json.\("createdAt").as[DateTime]
      ))
    }
  }
}
