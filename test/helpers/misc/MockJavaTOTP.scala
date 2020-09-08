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
