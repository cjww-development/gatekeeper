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

package global

import com.cjwwdev.mongo.indexing.RepositoryIndexer
import database.{AppStore, CodecReg, IndividualUserStore, OrganisationUserStore}
import javax.inject.Inject
import models.{RegisteredApplication, User}
import models.User._

import scala.concurrent.{ExecutionContext => ExC}

class GatekeeperIndexer @Inject()(val userStore: IndividualUserStore,
                                  val orgUserStore: OrganisationUserStore,
                                  val appStore: AppStore,
                                  implicit val ec: ExC) extends RepositoryIndexer with CodecReg {
  for {
    _ <- ensureMultipleIndexes[User](userStore)
    _ <- ensureMultipleIndexes[User](orgUserStore)
    _ <- ensureMultipleIndexes[RegisteredApplication](appStore)
  } yield true
}
