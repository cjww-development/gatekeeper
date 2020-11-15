package models

import org.bson.codecs.configuration.CodecProvider
import org.mongodb.scala.bson.codecs.Macros

case class Email(address: String,
                 verified: Boolean)

object Email {
  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[Email]()
}

