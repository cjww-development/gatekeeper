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

import helpers.Assertions
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest

class BasicAuthSpec extends PlaySpec with Assertions {

  val testBasicAuth = BasicAuth

  "decode" should {
    "return a user and pass" when {
      "given the correct auth header" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> "Basic dGVzdElkOnRlc3RTZWNyZXQ=")

        assertOutput(testBasicAuth.decode(req)) {
          _ mustBe Left("testId" -> "testSecret")
        }
      }
    }

    "return MalformedHeader" when {
      "given a suffix that's not Base64" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> "Basic invalidBasicAuthString")

        assertOutput(testBasicAuth.decode(req)) {
          _ mustBe Right(MalformedHeader)
        }
      }
    }

    "return a NoAuthHeader error" when {
      "the auth header isn't present" in {
        val req = FakeRequest()

        assertOutput(testBasicAuth.decode(req)) {
          _ mustBe Right(NoAuthHeader)
        }
      }
    }

    "return a InvalidPrefix error" when {
      "the auth header exists but isn't prefixed with Basic" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> "dGVzdElkOnRlc3RTZWNyZXQ=")

        assertOutput(testBasicAuth.decode(req)) {
          _ mustBe Right(InvalidPrefix)
        }
      }
    }
  }
}
