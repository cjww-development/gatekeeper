package models

import play.api.libs.json.Json

case class Gender(selection: String,
                  custom: Option[String])

object Gender {
  val toList = List("male", "female", "other", "not specified")
  implicit val format = Json.format[Gender]
}
