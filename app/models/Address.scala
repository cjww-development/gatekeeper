/*
 * Copyright 2022 CJWW Development
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

