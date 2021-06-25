/*
 * Copyright 2021 CJWW Development
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

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json, OFormat}
import utils.StringUtils._

import java.text.SimpleDateFormat
import java.util.Date

case class UserInfo(id: String,
                    userName: String,
                    email: String,
                    emailVerified: Boolean,
                    phone: Option[String],
                    phoneVerified: Boolean,
                    accType: String,
                    name: Name,
                    gender: Gender,
                    birthDate: Option[String],
                    address: Option[Address],
                    authorisedClients: List[AuthorisedClient],
                    mfaEnabled: Boolean,
                    createdAt: DateTime) {

  val toMap: Map[String, String] = Map(
   "id" -> id,
   "username" -> userName,
   "email" -> email,
   "act" -> accType
  )
}

object UserInfo extends TimeFormat {
  implicit val format: OFormat[UserInfo] = Json.format[UserInfo]

  val toOpenId: UserInfo => JsObject = userInfo => Json.obj(
    "sub" -> userInfo.id
  )

  val toProfile: UserInfo => JsObject = userInfo => {
    val concatName = Seq(userInfo.name.firstName, userInfo.name.middleName, userInfo.name.lastName).flatten.mkString(" ").trim
    val nameObj = if(concatName.isBlank) Json.obj() else Json.obj("name" -> concatName)

    val gender = if(userInfo.gender.selection == "not specified") {
      Json.obj()
    } else if(userInfo.gender.selection == "other") {
      userInfo.gender.custom.fold(Json.obj())(g => Json.obj("gender" -> g))
    } else {
      Json.obj("gender" -> userInfo.gender.selection)
    }

    nameObj ++
      userInfo.name.firstName.fold(Json.obj())(fn => Json.obj("given_name" -> fn)) ++
      userInfo.name.middleName.fold(Json.obj())(mn => Json.obj("middle_name" -> mn)) ++
      userInfo.name.lastName.fold(Json.obj())(fam => Json.obj("family_name" -> fam)) ++
      userInfo.name.nickName.fold(Json.obj())(nn => Json.obj("nickname" -> nn)) ++
      Json.obj("preferred_username" -> userInfo.userName) ++
      gender ++
      userInfo.birthDate.fold(Json.obj())(hdb => Json.obj("birthdate" -> hdb))
  }

  val toEmail: UserInfo => JsObject = userInfo => Json.obj(
    "email" -> userInfo.email,
    "email_verified" -> userInfo.emailVerified
  )

  val toPhone: UserInfo => JsObject = userInfo => userInfo.phone.fold(Json.obj()) { num =>
    Json.obj(
      "phone_number" -> num,
      "phone_number_verified" -> userInfo.phoneVerified
    )
  }

  val toAddress: UserInfo => JsObject = _.address.fold(Json.obj()) { addr =>
    val street = if(addr.streetAddress.nonEmpty) Json.obj("street_address" -> addr.streetAddress) else Json.obj()
    val locality = if(addr.locality.nonEmpty) Json.obj("locality" -> addr.locality) else Json.obj()
    val region = if(addr.region.nonEmpty) Json.obj("region" -> addr.region) else Json.obj()
    val postCode = if(addr.postalCode.nonEmpty) Json.obj("postal_code" -> addr.postalCode) else Json.obj()
    val country = if(addr.country.nonEmpty) Json.obj("country" -> addr.country) else Json.obj()

    val addrObj = Json.obj("formatted" -> addr.formatted) ++
      street ++
      locality ++
      region ++
      postCode ++
      country

    Json.obj("address" -> addrObj)
  }

  val fromUser: User => UserInfo = user => UserInfo(
    id = user.id,
    userName = user.userName.decrypt.getOrElse(""),
    email = user.digitalContact.email.address.decrypt.getOrElse(""),
    emailVerified = user.digitalContact.email.verified,
    phone = user.digitalContact.phone.map(_.number),
    phoneVerified = user.digitalContact.phone.exists(_.verified),
    accType = user.accType,
    name = Name(
      firstName = user.profile.flatMap(_.givenName),
      middleName = user.profile.flatMap(_.middleName),
      lastName = user.profile.flatMap(_.familyName),
      nickName = user.profile.flatMap(_.nickname)
    ),
    gender = Gender(
      selection = ???,
      custom = ???
    ),
    birthDate = user.profile.flatMap(_.birthDate.map { date =>
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
      dateFormat.format(date)
    }),
    address = user.address,
    authorisedClients = user.authorisedClients,
    mfaEnabled = user.mfaEnabled,
    createdAt = user.createdAt
  )
}
