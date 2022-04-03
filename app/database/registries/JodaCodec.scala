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

package database.registries

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import org.joda.time.DateTime

class JodaCodec extends Codec[DateTime] {
  override def decode(bsonReader: BsonReader, decoderContext: DecoderContext): DateTime = new DateTime(bsonReader.readDateTime())
  override def encode(bsonWriter: BsonWriter, t: DateTime, encoderContext: EncoderContext): Unit = bsonWriter.writeDateTime(t.getMillis)
  override def getEncoderClass: Class[DateTime] = classOf[DateTime]
}