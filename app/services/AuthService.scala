/*
 * Copyright 2019 CJWW Development
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

import java.util.UUID

import database.{AuthStore, RegisteredApplicationsStore, UserStore}
import javax.inject.Inject
import models.{AuthCode, AuthCodeRequest}
import models.RegisteredApplication._

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultAuthService @Inject()(val userStore: UserStore,
                                   val appStore: RegisteredApplicationsStore,
                                   val authStore: AuthStore) extends AuthService

trait AuthService {

  val userStore: UserStore
  val appStore: RegisteredApplicationsStore
  val authStore: AuthStore

  def generateAuthCode(req: AuthCodeRequest)(implicit ec: ExC): Future[AuthCode] = {
    if(req.responseType != "code") {
      throw new Exception("Unsupported response type")
    }

    appStore.getOneApplication("clientId", req.clientId) flatMap {
      case Some(_) => {
        val newAuthCode = AuthCode(UUID.randomUUID().toString, req.state)
        authStore.saveCode(newAuthCode) map {
          _ => newAuthCode
        }
      }
      case None => throw new Exception("No matching registered application")
    }
  }

  def issueToken(email: String, code: String, state: String)(implicit ec: ExC): Future[Either[Boolean, String]] = {
    authStore.getCode(code, state) map {
      if(_) {
        Right("This is crap OAuth2 token")
      } else {
        Left(false)
      }
    }
  }
}
