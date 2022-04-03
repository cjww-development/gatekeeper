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

import models.Gender
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}

object GenderForm {
  def genderEnum: Mapping[String] = {
    val genderConstraint: Constraint[String] = Constraint("constraint.selection")({ gender =>
      val errors = gender match {
        case "male" | "female" | "other" | "not specified" => Nil
        case _ => Seq(ValidationError("Invalid selection"))
      }
      if(errors.isEmpty) Valid else Invalid(errors)
    })
    text.verifying(genderConstraint)
  }

  val form: Form[Gender] = Form(
    mapping(
      "selection" -> genderEnum,
      "custom"    -> optional(text)
    )(Gender.apply)(Gender.unapply)
  )
}
