package models

import java.util.Date

import org.bson.codecs.configuration.CodecProvider
import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.Macros

case class Profile(name: Option[String],
                   familyName: Option[String],
                   givenName: Option[String],
                   middleName: Option[String],
                   nickname: Option[String],
                   preferredUsername: Option[String],
                   profile: Option[String],
                   picture: Option[String],
                   website: Option[String],
                   gender: Option[String],
                   birthDate: Option[Date],
                   zoneinfo: Option[String],
                   locale: Option[String],
                   updatedAt: Option[DateTime])

object Profile {
  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[Profile]()
}
