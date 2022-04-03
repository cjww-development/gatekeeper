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

package forms

import models.RegisteredApplication
import play.api.data.Form
import play.api.data.Forms._

object AppRegistrationForm {
  def form(owner: String): Form[RegisteredApplication] = Form(
    mapping(
      "name" -> text,
      "desc" -> text,
      "homeUrl" -> text,
      "redirectUrl" -> text,
      "clientType" -> text,
      "iconUrl" -> optional(text)
    )(RegisteredApplication.apply(owner, _, _, _, _, _, _))(RegisteredApplication.unapply)
  )
}
