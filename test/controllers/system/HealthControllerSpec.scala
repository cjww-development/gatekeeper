///*
// * Copyright 2020 CJWW Development
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers.system
//
//import helpers.Assertions
//import org.scalatestplus.play.PlaySpec
//import play.api.mvc.ControllerComponents
//import play.api.test.FakeRequest
//import play.api.test.Helpers._
//
//class HealthControllerSpec
//  extends PlaySpec
//    with Assertions {
//
//  val testController: HealthController = new HealthController {
//    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
//  }
//
//  "ping" should {
//    "return an Ok" in {
//      assertFutureResult(testController.ping()(FakeRequest())) { res =>
//        status(res) mustBe OK
//      }
//    }
//  }
//}
