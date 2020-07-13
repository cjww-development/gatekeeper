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

import helpers.Assertions
import org.scalatestplus.play.PlaySpec

class UserSpec extends PlaySpec with Assertions {

  "User.apply" should {
    "return a User model with extra fields" in {
      val testUser = User(
        userName = "testUser",
        email = "test@email.com",
        accType = "individual",
        password = "testing"
      )

      assertOutput(testUser) { res =>
        assert(res.userName != "testUser")
        assert(res.email != "test@email.com")
        res.accType mustBe "individual"
        res.salt.length mustBe 32
        assert(res.password != "testing")
        res.password.length mustBe 128
      }
    }

    "return a User model with extra fields that weren't trimmed" in {
      val testUser = User(
        userName = " testUser  ",
        email = "   test@email.com ",
        accType = "individual",
        password = "testing"
      )

      assertOutput(testUser) { res =>
        assert(res.userName != "testUser")
        assert(res.email != "test@email.com")
        res.accType mustBe "individual"
        res.salt.length mustBe 32
        assert(res.password != "testing")
        res.password.length mustBe 128
      }
    }
  }
}
