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

package helpers

import org.scalatest.Assertion
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.Future
import scala.reflect.ClassTag

trait Assertions extends FutureAwaits with DefaultAwaitTimeout {
  self: PlaySpec =>

  def assertOutput[T](underTest: T)(assertions: T => Assertion): Assertion = {
    assertions(underTest)
  }

  def awaitAndAssert[T](underTest: => Future[T])(assertions: T => Assertion): Assertion = {
    assertions(await(underTest))
  }

  def awaitAndIntercept[X <: AnyRef](methodUnderTest: => Future[Any])(implicit classTag: ClassTag[X]): X = {
    intercept[X](await(methodUnderTest))
  }
}
