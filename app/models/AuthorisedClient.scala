package models

import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.Macros
import play.api.libs.json.{Json, OFormat}

import scala.reflect.ClassTag

case class AuthorisedClient(appId: String,
                            authorisedScopes: Seq[String],
                            authorisedOn: DateTime)

object AuthorisedClient extends TimeFormat {
  val codec = Macros.createCodecProviderIgnoreNone[AuthorisedClient]()

  implicit val classTag: ClassTag[AuthorisedClient] = ClassTag[AuthorisedClient](classOf[AuthorisedClient])

  implicit val scopeFormat: OFormat[Scope] = Json.format[Scope]
  implicit val format: OFormat[AuthorisedClient] = Json.format[AuthorisedClient]
}
