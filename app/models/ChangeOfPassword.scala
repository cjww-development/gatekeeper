package models

case class ChangeOfPassword(oldPassword: String,
                            newPassword: String,
                            confirmedPassword: String)


