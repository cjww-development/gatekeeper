package models

case class AuthRequest(responseType: String,              //code for returning an auth code, token when requesting an access token using the implicit flow
                       redirectUri: String,               //Pulled from questing the client store
                       clientId: String,                  //Included in the request
                       scope: Seq[String],                //What the client is looking to access
                       userId: String)                    //Found in the cookie
