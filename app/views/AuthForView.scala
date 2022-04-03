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

package views

import models.ServerCookies._
import play.api.mvc.RequestHeader

object AuthForView {

  def isAuthenticated(rh: RequestHeader): Boolean = {
    rh
      .cookies
      .get("aas")
      .isDefined
  }

  def isOrgUser(rh: RequestHeader): Boolean = {
    rh
      .cookies
      .get("aas")
      .fold(false) { cookie =>
        val id = cookie.getValue()
        id.startsWith("user-") -> id.startsWith("org-user-") match {
          case (true, false) => false
          case (false, true) => true
          case (_, _) => false
        }
      }
  }
}
