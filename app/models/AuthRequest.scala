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

package models

case class AuthRequest(responseType: String,              //code for returning an auth code, token when requesting an access token using the implicit flow
                       redirectUri: String,               //Pulled from questing the client store
                       clientId: String,                  //Included in the request
                       scope: Seq[String],                //What the client is looking to access
                       userId: String)                    //Found in the cookie
