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

import com.typesafe.config.ConfigFactory

import scala.util.Try

trait EncoderConfig {
  protected lazy val appName      = Try(ConfigFactory.load().getString("appName")).fold(_ => "-", identity)
  protected val DATE_FORMAT       = Try(ConfigFactory.load().getString("logging.dateFormat")).fold(_ => "yyyy-MM-dd HH:mm:ss.SSSZZ", identity)
  protected val requestTypeRegex  = """(HEAD|GET|POST|PUT|PATCH|DELETE) request to (.*) returned a \d{3} and took \d+ms"""
  protected val requestIdRegex    = """requestId=\[((requestId-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})|(-))\]"""
  protected val outboundTypeRegex = """Outbound (HEAD|GET|POST|PUT|PATCH|DELETE) call to (.*) returned a \d{3}"""
  protected val outboundHostRegex = """^(http|https):\/\/(.*)$"""
}
