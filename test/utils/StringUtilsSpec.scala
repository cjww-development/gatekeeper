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

package utils

import org.scalatestplus.play.PlaySpec

class StringUtilsSpec extends PlaySpec {

  "salter" should {
    "produce a random string of 8 chars long" in {
      val result = StringUtils.salter(length = 8)
      result.length mustBe 8
    }

    "produce a random string of 16 chars long" in {
      val result = StringUtils.salter(length = 16)
      result.length mustBe 16
    }

    "produce a random string of 32 chars long" in {
      val result = StringUtils.salter(length = 32)
      result.length mustBe 32
    }
  }

  "hasher" should {
    val salt = StringUtils.salter(length = 8)

    "not return the same hash when using different salts on the same input" in {
      val saltOne = StringUtils.salter(length = 8)
      val saltTwo = StringUtils.salter(length = 8)

      val resultOne = StringUtils.hasher(saltOne, "test")
      val resultTwo = StringUtils.hasher(saltTwo, "test")

      assert(resultOne != resultTwo)
    }

    "return the same hash when using the same salt on the same input" in {
      val resultOne = StringUtils.hasher(salt, "test")
      val resultTwo = StringUtils.hasher(salt, "test")

      assert(resultOne == resultTwo)
    }

    "return a string exactly 128 chars long and isn't equal to what was input" in {
      val input = "aaaaa"
      val result = StringUtils.hasher(salt, input)
      assert(result != input)
    }

    "encrypt a string that is five chars long and still return a 128 char string" in {
      val input = "aaaaa"
      val result = StringUtils.hasher(salt, input)
      result.length mustBe 128
    }

    "encrypt a string that is ten chars long and still return a 128 char string" in {
      val input = "aaaaaaaaaa"
      val result = StringUtils.hasher(salt, input)
      result.length mustBe 128
    }

    "encrypt a string that is twenty chars long and still return a 128 char string" in {
      val input = "aaaaaaaaaaaaaaaaaaaa"
      val result = StringUtils.hasher(salt, input)
      result.length mustBe 128
    }
  }
}
