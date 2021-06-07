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

import dev.cjww.security.Implicits._
import dev.cjww.security.deobfuscation.DeObfuscators
import dev.cjww.security.obfuscation.Obfuscators
import org.bson.codecs.configuration.CodecProvider
import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.Macros
import utils.StringUtils

import java.util.UUID
import scala.reflect.ClassTag

case class User(id: String,
                userName: String,
                digitalContact: DigitalContact,
                profile: Option[Profile],
                address: Option[Address],
                accType: String,
                salt: String,
                password: String,
                authorisedClients: List[AuthorisedClient],
                mfaEnabled: Boolean,
                mfaSecret: Option[String],
                createdAt: DateTime)

object User extends Obfuscators with DeObfuscators {
  override val locale: String = "models.User"

  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[User]()

  implicit val classTag: ClassTag[User] = ClassTag[User](classOf[User])

  def apply(userName: String, email: String, accType: String, password: String): User = {
    val saltStr = StringUtils.salter(length = 32)

    val id = accType.trim match {
      case "individual"   => s"user-${UUID.randomUUID()}"
      case "organisation" => s"org-user-${UUID.randomUUID()}"
    }

    new User(
      id,
      userName.trim.encrypt,
      digitalContact = DigitalContact(
        email = Email(
          address = email.trim.encrypt,
          verified = false
        ),
        phone = None
      ),
      profile = None,
      address = None,
      accType.trim,
      salt = saltStr,
      password = StringUtils.hasher(saltStr, password),
      authorisedClients = List(),
      mfaEnabled = false,
      mfaSecret = None,
      createdAt = DateTime.now()
    )
  }

  def unapply(arg: User): Option[(String, String, String, String)] = {
    Some((arg.userName, arg.digitalContact.email.address, arg.accType, arg.password))
  }
}
