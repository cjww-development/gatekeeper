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

import com.cjwwdev.mongo.responses.MongoCreateResponse
import com.cjwwdev.security.sha.SHA512
import database.UserStore
import javax.inject.Inject
import models.User

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultUserService @Inject()(val userStore: UserStore) extends UserService

trait UserService {

  val userStore: UserStore

  def createUser(user: User): Future[MongoCreateResponse] = {
    val encUser = User(
      name = user.name,
      email = user.email,
      password = SHA512.encrypt(user.password),
      username = user.username
    )
    userStore.createUser(encUser)
  }
  def login(username: String, password: String)(implicit ec: ExC): Future[Option[User]] = {
    userStore.getUser("username", username) map {
      case Some(user) => if(user.password == password) Some(user) else None
      case None => None
    }
  }
}
