package models

import org.bson.codecs.configuration.CodecProvider
import org.mongodb.scala.bson.codecs.Macros

case class Phone(number: String,
                 verified: Boolean)

object Phone {
  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[Phone]()
}
