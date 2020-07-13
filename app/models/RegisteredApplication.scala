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

import com.cjwwdev.security.Implicits._
import com.cjwwdev.security.obfuscation.Obfuscators
import play.api.libs.json._

import scala.collection.Seq

case class RegisteredApplication(owner: String,
                                 name: String,
                                 desc: String,
                                 homeUrl: String,
                                 redirectUrl: String,
                                 clientType: String,
                                 clientId: String,
                                 clientSecret: Option[String])

object RegisteredApplication extends Obfuscators {
  override val locale: String = this.getClass.getCanonicalName

  private def generateIds(iterations: Int): String = {
    (0 to iterations)
      .map(_ => UUID.randomUUID().toString.replace("-", ""))
      .mkString
      .encrypt
  }

  implicit class RegisteredApplicationOps(app: RegisteredApplication) {
    def regenerateIdsAndSecrets: RegisteredApplication = {
      app.copy(
        clientId = generateIds(iterations = 1),
        clientSecret = app.clientType match {
          case "confidential" => Some(generateIds(iterations = 2))
          case "public"       => None
        }
      )
    }
  }

  def apply(name: String, desc: String, homeUrl: String, redirectUrl: String, clientType: String): RegisteredApplication = {
    new RegisteredApplication(
      "testOwner",
      name,
      desc,
      homeUrl,
      redirectUrl,
      clientType,
      generateIds(iterations = 1),
      clientType match {
        case "confidential" => Some(generateIds(iterations = 2))
        case "public"       => None
      }
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
        }
      ))
    }
  }
}
