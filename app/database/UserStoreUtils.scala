package database

trait UserStoreUtils {

  protected val individualUserStore: UserStore
  protected val organisationUserStore: UserStore

  protected def getUserStore(userId: String): UserStore = {
    userId match {
      case str if str.startsWith("user-") => individualUserStore
      case str if str.startsWith("org-user-") => organisationUserStore
    }
  }
}
