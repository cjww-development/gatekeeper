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

sealed trait LogType
case class RequestLog(method: String, status: Int, duration: Int) extends LogType
case class OutboundLog(method: String, status: Int, outboundHost: String) extends LogType
case object StandardLog extends LogType

trait EncodingUtils {
  self: EncoderConfig =>

  private val httpVerbs = List("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")

  private val requestLog: String => RequestLog = msg => {
    val splitMessage = msg.split(" ").toList
    RequestLog(
      method   = splitMessage.find(httpVerbs.contains).getOrElse("-"),
      status   = splitMessage(splitMessage.length - 4).toInt,
      duration = splitMessage.last.replace("ms", "").toInt
    )
  }

  private val outboundLog: String => OutboundLog = msg => {
    val splitMessage = msg.split(" ").toList
    OutboundLog(
      method = splitMessage.find(httpVerbs.contains).getOrElse("-"),
      status = splitMessage.last.toInt,
      outboundHost = splitMessage
        .find(_.matches(outboundHostRegex))
        .map(_.replace("http://", "").replace("https://", "").replace(""":\d{4}""", ""))
        .map(_.split("/").toList)
        .map(_.head)
        .getOrElse("-")
    )
  }

  protected val getLogType: String => LogType = {
    case x if x.matches(requestTypeRegex)  => requestLog(x)
    case x if x.matches(outboundTypeRegex) => outboundLog(x)
    case _                                 => StandardLog
  }
}
