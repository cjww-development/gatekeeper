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

package global

import ch.qos.logback.classic.spi.{ILoggingEvent, ThrowableProxyUtil}
import ch.qos.logback.core.encoder.EncoderBase
import com.fasterxml.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.time.FastDateFormat

import java.net.InetAddress
import java.nio.charset.StandardCharsets

class JsonLoggingEncoder extends EncoderBase[ILoggingEvent] with EncoderConfig with EncodingUtils {

  private val mapper = new ObjectMapper().configure(ESCAPE_NON_ASCII.mappedFeature(), true)

  override def encode(event: ILoggingEvent): Array[Byte] = {
    val eventNode = mapper.createObjectNode()

    val host = InetAddress.getLocalHost

    val loggingContent: Map[String, String] = Map(
      "timestamp"      -> FastDateFormat.getInstance(DATE_FORMAT).format(event.getTimeStamp),
      "service"        -> appName,
      "hostname"       -> host.getHostName,
      "dockerIp"       -> host.getHostAddress,
      "serviceVersion" -> System.getProperty("version", "-"),
      "logger"         -> event.getLoggerName,
      "level"          -> event.getLevel.levelStr,
      "thread"         -> event.getThreadName,
      "message"        -> event.getMessage.replaceAll(requestIdRegex, "").trim
    )

    getLogType(event.getMessage.replaceAll(requestIdRegex, "").trim) match {
      case RequestLog(method, status, duration) =>
        eventNode.put("logType", "request")
        eventNode.put("method", method)
        eventNode.put("status", status)
        eventNode.put("duration", duration)
      case OutboundLog(method, status, outboundHost) =>
        eventNode.put("logType", "outbound")
        eventNode.put("method", method)
        eventNode.put("status", status)
        eventNode.put("outboundHost", outboundHost)
      case StandardLog =>
        eventNode.put("logType", "standard")
    }

    event.getMDCPropertyMap.forEach((k, v) => eventNode.put(k, v))

    Option(event.getThrowableProxy).map(e => eventNode.put("exception", ThrowableProxyUtil.asString(e)))

    loggingContent.foreach { case (key, content) => eventNode.put(key, content) }

    s"${mapper.writeValueAsString(eventNode)}${System.lineSeparator}".getBytes(StandardCharsets.UTF_8)
  }

  override def headerBytes(): Array[Byte] = System.lineSeparator.getBytes(StandardCharsets.UTF_8)
  override def footerBytes(): Array[Byte] = System.lineSeparator.getBytes(StandardCharsets.UTF_8)
}
