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

package utils

import dev.cjww.security.DecryptionError
import dev.cjww.security.defence.DataDefenders
import io.github.nremond.PBKDF2

import java.nio.charset.StandardCharsets
import scala.util.Random

object StringUtils {
  def salter(length: Int): String = {
    val r = new Random()
    (0 until length)
      .map(_ => r.nextPrintableChar())
      .mkString
  }

  def hasher(salt: String, input: String): String = {
    PBKDF2(input.getBytes(StandardCharsets.UTF_8), salt.getBytes(StandardCharsets.UTF_8), 20000, 64, "HmacSHA512")
      .map(x => "%02x".format(x))
      .mkString
  }

  implicit class ImplicitStringUtils(data: String) extends DataDefenders {
    override val locale: String = ""
    def encrypt: String = stringDefense.encrypt(data)
    def decrypt: Either[DecryptionError, String] = stringDefense.decrypt(data)
  }
}
