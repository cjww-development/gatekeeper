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

import org.slf4j.LoggerFactory
import play.api.data.Form
import play.api.data.Forms._

case class MFACode(code: String)

object MFAForm {

  private val logger = LoggerFactory.getLogger(this.getClass)

  val mfaForm: Form[MFACode] = Form(
    mapping(
      "code" -> nonEmptyText
    )(MFACode.apply)(MFACode.unapply)
  )

  implicit class MFAFormOps(mfaForm: Form[MFACode]) {
    def renderErrors: Form[MFACode] = {
      logger.warn(s"[renderErrors] - MFACode form was in error; user failed MFA challenge")
      mfaForm
        .withError("code", "Enter the generated code")
    }

    def renderInvalidErrors: Form[MFACode] = {
      logger.warn(s"[renderInvalidErrors] - MFACode form was in error; user failed MFA challenge")
      mfaForm
        .withError("code", "This code was invalid")
    }
  }
}
