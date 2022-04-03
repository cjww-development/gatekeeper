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

package services.oauth2

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk._
import com.nimbusds.jose.jwk.gen._
import play.api.Configuration

import java.util.UUID
import javax.inject.Inject

class DefaultJwksService @Inject()(val config: Configuration) extends JwksService {
  override protected val rsaKeyGenerator: RSAKeyGenerator = new RSAKeyGenerator(2048)
  override val getCurrentJwks: RSAKey = {
    rsaKeyGenerator
      .keyUse(KeyUse.SIGNATURE)
      .algorithm(new Algorithm("RS256"))
      .keyID(UUID.randomUUID().toString)
      .generate()
  }
}

trait JwksService {

  protected val rsaKeyGenerator: RSAKeyGenerator

  val getCurrentJwks: RSAKey
}
