package models

import play.api.libs.json.Json

case class Name(firstName: Option[String],
                middleName: Option[String],
                lastName: Option[String])

object Name {
  implicit val format = Json.format[Name]
}
