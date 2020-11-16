package models

import org.bson.codecs.configuration.CodecProvider
import org.mongodb.scala.bson.codecs.Macros
import play.api.libs.json.{Json, OFormat}

case class Address(formatted: String,
                   streetAddress: String,
                   locality: String,
                   region: String,
                   postalCode: String,
                   country: String) {
  def isEmpty: Boolean = {
    streetAddress.isEmpty && locality.isEmpty && region.isEmpty && postalCode.isEmpty && country.isEmpty
  }
}

object Address {
  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[Address]()

  implicit val format: OFormat[Address] = Json.format[Address]

  def apply(formatted: String, streetAddress: String, locality: String, region: String, postalCode: String, country: String): Address = new Address(formatted, streetAddress, locality, region, postalCode, country)

  def apply(streetAddress: String, locality: String, region: String, postalCode: String, country: String): Address = {
    new Address(
      Seq(streetAddress.trim, locality.trim, region.trim, postalCode.trim, country.trim)
        .filterNot(_.isBlank)
        .mkString("\n"),
      streetAddress,
      locality,
      region,
      postalCode,
      country
    )
  }

  def formUnapply(arg: Address): Option[(String, String, String, String, String)] = {
    Some((arg.streetAddress, arg.locality, arg.region, arg.postalCode, arg.country))
  }
}

