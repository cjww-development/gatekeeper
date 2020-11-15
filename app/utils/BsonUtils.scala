package utils

import org.mongodb.scala.bson.BsonDocument

object BsonUtils {
  implicit class BsonOps(doc: BsonDocument) {
    def getOptionalString(key: String): Option[String] = {
      val value = doc.get(key)
      if(value == null) None else Some(value.asString().getValue)
    }

    def getOptionalDocument(key: String): Option[BsonDocument] = {
      val value = doc.get(key)
      if(value == null) None else Some(value.asDocument())
    }

    def getOptionalBoolean(key: String): Option[Boolean] = {
      val value = doc.get(key)
      if(value == null) None else Some(value.asBoolean().getValue)
    }
  }
}
