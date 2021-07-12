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

package helpers.misc

import dev.samstevens.totp.code.CodeVerifier
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.SecretGenerator
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

trait MockJavaTOTP extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSecretGenerator)
    reset(mockQrGenerator)
    reset(mockCodeVerifier)
  }

  val mockSecretGenerator: SecretGenerator = mock[SecretGenerator]
  val mockQrGenerator: ZxingPngQrGenerator = mock[ZxingPngQrGenerator]
  val mockCodeVerifier: CodeVerifier = mock[CodeVerifier]

  def mockGenerateSecret(): OngoingStubbing[String] = {
    when(mockSecretGenerator.generate())
      .thenReturn("testSecret")
  }

  def mockIsValidCode(isValid: Boolean): OngoingStubbing[Boolean] = {
    when(mockCodeVerifier.isValidCode(ArgumentMatchers.any[String](), ArgumentMatchers.any[String]()))
      .thenReturn(isValid)
  }
}
