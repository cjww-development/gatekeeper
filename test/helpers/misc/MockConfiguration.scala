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

package helpers.misc

import models.{Login, User}
import orchestrators.LoginOrchestrator
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.{ConfigLoader, Configuration}

import scala.concurrent.Future

trait MockConfiguration extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockConfiguration: Configuration = mock[Configuration]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConfiguration)
  }

  def mockGetConfig[T](value: T): OngoingStubbing[T] = {
    when(mockConfiguration.get[T](ArgumentMatchers.any[String]())(ArgumentMatchers.any[ConfigLoader[T]]()))
      .thenReturn(value)
  }

  def mockMultipleGetConfig[T](valueOne: T, valueTwo: T): OngoingStubbing[T] = {
    when(mockConfiguration.get[T](ArgumentMatchers.any[String]())(ArgumentMatchers.any[ConfigLoader[T]]()))
      .thenReturn(valueOne)
      .thenReturn(valueTwo)
  }
}
