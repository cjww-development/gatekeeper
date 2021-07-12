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

package database

import dev.cjww.mongo.responses.MongoSuccessCreate
import helpers.{Assertions, IntegrationApp}
import models.JwksContainer
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global

class JwksStoreISpec extends PlaySpec with IntegrationApp with Assertions with BeforeAndAfterAll with CodecReg {

  val testJwksStore: JwksStore = app.injector.instanceOf[JwksStore]

  val testJwksContainer: JwksContainer = JwksContainer(
    kid = "testKid",
    jwk = "testJwk"
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(testJwksStore.collection[JwksContainer].drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(testJwksStore.collection[JwksContainer].drop().toFuture())
  }

  "createJwks" should {
    "return a MongoSuccessCreate" when {
      "a new Jwks has been created" in {
        awaitAndAssert(testJwksStore.createJwks(testJwksContainer)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }
  }

  "getAllJwks" should {
    "return a Sequence of JWKs" when {
      "there are apps owned by the user" in {
        awaitAndAssert(testJwksStore.getAllJwks) {
          _ mustBe Seq(testJwksContainer)
        }
      }
    }
  }
}
