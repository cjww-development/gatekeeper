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
import org.mongodb.scala.bson.codecs.Macros

import java.util.UUID

case class LoginAttempt(id: String,
                        userId: String,
                        success: Boolean,
                        createdAt: DateTime)

object LoginAttempt {

  val codec = Macros.createCodecProviderIgnoreNone[LoginAttempt]()

  def apply(id: String, userId: String, success: Boolean, createdAt: DateTime): LoginAttempt = {
    new LoginAttempt(id, userId, success, createdAt)
  }

  def apply(userId: String, success: Boolean): LoginAttempt = new LoginAttempt(
    id = s"att-${UUID.randomUUID().toString}",
    userId,
    success,
    createdAt = DateTime.now()
  )
}
