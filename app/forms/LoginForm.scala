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

package forms

import models.Login
import org.slf4j.LoggerFactory
import play.api.data.Form
import play.api.data.Forms._

object LoginForm {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val form: Form[Login] = Form(
    mapping(
      "userName" -> text,
      "password" -> text
    )(Login.apply)(Login.unapply)
  )

  implicit class RegistrationFormOps(loginForm: Form[Login]) {
    def renderErrors: Form[Login] = {
      logger.warn(s"[renderErrors] - Login form was in error; user not authenticated")
      loginForm
        .withError("userName", "Check your user name or password")
        .withError("password", "Check your user name or password")
    }
  }
}
