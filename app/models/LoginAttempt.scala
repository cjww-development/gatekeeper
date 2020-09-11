package models

import java.util.UUID

import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.Macros

case class LoginAttempt(id: String,
                        userId: String,
                        success: Boolean,
                        createdAt: DateTime)

object LoginAttempt {

  val codec = Macros.createCodecProviderIgnoreNone[LoginAttempt]()

  def apply(id: String, userId: String, success: Boolean, createdAt: DateTime): LoginAttempt = {
    new LoginAttempt(id, userId, success, createdAt)
  }

  def apply(userId: String, success: Boolean): LoginAttempt = new LoginAttempt(
    id = s"att-${UUID.randomUUID().toString}",
    userId,
    success,
    createdAt = DateTime.now()
  )
}
