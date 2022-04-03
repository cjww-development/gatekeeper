/*
 * Copyright 2022 CJWW Development
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

    def getOptionalLong(key: String): Option[Long] = {
      val value = doc.get(key)
      if(value == null) None else Some(value.asDateTime().getValue)
    }
  }
}
