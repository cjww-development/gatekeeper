package models

import org.bson.codecs.configuration.CodecProvider
import org.mongodb.scala.bson.codecs.Macros

case class DigitalContact(email: Email,
                          phone: Option[Phone])

object DigitalContact {
  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[DigitalContact]()
}
