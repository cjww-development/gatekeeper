package views

import play.api.mvc.RequestHeader
import models.ServerCookies._

object AuthForView {

  def isAuthenticated(rh: RequestHeader): Boolean = {
    rh
      .cookies
      .get("aas")
      .isDefined
  }

  def isOrgUser(rh: RequestHeader): Boolean = {
    rh
      .cookies
      .get("aas")
      .fold(false) { cookie =>
        val id = cookie.getValue()
        id.startsWith("user-") -> id.startsWith("org-user-") match {
          case (true, false) => false
          case (false, true) => true
          case (_, _) => false
        }
      }
  }
}
