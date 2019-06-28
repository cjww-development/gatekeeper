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

package database.tables

import models.RegisteredApplication
import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

class RegisteredApplications(tag: Tag) extends Table[RegisteredApplication](tag, "registered_applications") {
  private val nameLength         = 50
  private val urlLength          = 100
  private val clientTypeLength   = 12
  private val clientIdLength     = 64
  private val clientSecretLength = 107

  def id: Rep[Int]                      = column[Int]("id",                       O.PrimaryKey,                 O.AutoInc)
  def name: Rep[String]                 = column[String]("name",                  O.Length(nameLength),         O.Unique )
  def description: Rep[String]          = column[String]("description"                                                   )
  def homeUrl: Rep[String]              = column[String]("home_url",              O.Length(urlLength)                    )
  def redirectUrl: Rep[String]          = column[String]("redirect_url",          O.Length(urlLength)                    )
  def clientType: Rep[String]           = column[String]("client_type",           O.Length(clientTypeLength)             )
  def clientId: Rep[String]             = column[String]("client_id",             O.Length(clientIdLength),     O.Unique )
  def clientSecret: Rep[Option[String]] = column[Option[String]]("client_secret", O.Length(clientSecretLength), O.Unique )

  override def * : ProvenShape[RegisteredApplication] = {
    (name, description, homeUrl, redirectUrl, clientType, clientId, clientSecret)<>
      (RegisteredApplication.tupled, RegisteredApplication.unapply)
  }
}
