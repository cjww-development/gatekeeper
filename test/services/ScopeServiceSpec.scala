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

package services

import helpers.Assertions
import helpers.misc.MockConfiguration
import models.Scopes
import org.scalatestplus.play.PlaySpec
import play.api.Configuration

class ScopeServiceSpec
  extends PlaySpec
    with Assertions
    with MockConfiguration {

  private val testService: ScopeService = new ScopeService {
    override val config: Configuration = mockConfiguration
  }

  "getValidScopes" should {
    "return a Scopes object" in {
      mockMultipleGetConfig[Seq[String]](
        valueOne = Seq("testReadValue"),
        valueTwo = Seq("testWriteValue"),
      )

      assertOutput(testService.getValidScopes) {
        _ mustBe Scopes(
          reads  = Seq("testReadValue"),
          writes = Seq("testWriteValue"),
        )
      }
    }
  }
}
