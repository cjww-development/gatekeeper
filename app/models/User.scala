/*
 * Copyright 2020 CJWW Development
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

import java.util.UUID

import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import com.cjwwdev.security.sha.SHA512
import com.cjwwdev.security.defence.DataDefenders

import scala.reflect.ClassTag

case class User(id: String,
                userName: String,
                email: String,
                accType: String,
                password: String)

object User extends DataDefenders {
  override val locale: String = this.getClass.getCanonicalName

  implicit val codec: CodecRegistry = fromRegistries(fromProviders(classOf[User]), DEFAULT_CODEC_REGISTRY)
  implicit val classTag: ClassTag[User] = ClassTag[User](classOf[User])

  def apply(userName: String, email: String, accType: String, password: String): User = {
    val id = accType match {
      case "Individual"   => s"user-${UUID.randomUUID()}"
      case "Organisation" => s"org-user-${UUID.randomUUID()}"
    }
    new User(id, userName, stringDefense.encrypt(email), accType, SHA512.encrypt(password))
  }

  def unapply(arg: User): Option[(String, String, String, String)] = {
    Some((arg.userName, arg.email, arg.accType, arg.password))
  }
}
