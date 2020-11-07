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

package database

import database.registries.JodaCodec
import models.{AuthorisedClient, EmailVerification, Grant, LoginAttempt, Profile, RegisteredApplication, Scope, TokenRecord, User}
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

trait CodecReg {
  implicit val codec: CodecRegistry = fromRegistries(
    fromCodecs(new JodaCodec),
    fromProviders(
      User.codec,
      RegisteredApplication.codec,
      Grant.codec,
      LoginAttempt.codec,
      AuthorisedClient.codec,
      Scope.codec,
      TokenRecord.codec,
      EmailVerification.codec,
      Profile.codec
    ),
    DEFAULT_CODEC_REGISTRY
  )
}
