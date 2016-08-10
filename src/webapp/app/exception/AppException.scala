package exception

import exception.ExceptionCodes._
import play.api.i18n.Messages

case class AppException(code: Int, underlying: Option[Throwable]) extends Exception {

  override def toString = underlying.map(message + "\n" + _.toString).getOrElse(message)

  def message = {
    Messages.apply(code match {
      case USER_NOT_FOUND => USER_NOT_FOUND.toString
      case TOO_MANY_LOGIN_ATTEMPTS_ACCOUNT_LOCKED => TOO_MANY_LOGIN_ATTEMPTS_ACCOUNT_LOCKED.toString
      case TICKET_PARSING_WRONG_DATE => "error.ticket.parsing.wrong.date"
      case TICKET_PARSING_CORRUPT_UPLOAD => "error.ticket.parsing.corrupt.upload"
      case ROLE_IS_MISSING => "role.missing"
      case UNSPECIFIC => "error.unspecific"
      case EMAIL_NOT_SENT => "error.email.could.not.be.sent"
      case _ => "error.unspecific"
    })
  }
}

object AppException {

  def apply = new AppException(UNSPECIFIC, None)

  def apply(code: Int) = new AppException(code, None)

}

object ExceptionCodes {
  val UNSPECIFIC = 1
  val TICKET_PARSING_WRONG_DATE = 2
  val TICKET_PARSING_CORRUPT_UPLOAD = 9
  val ROLE_IS_MISSING = 3
  val EMAIL_NOT_SENT = 4
  val TOO_MANY_LOGIN_ATTEMPTS_ACCOUNT_LOCKED = 5
  val USER_NOT_FOUND = 6
  val MAX_LOGIN_ATTEMPTS_INCREMENTED = 7
  val WRONG_USERNAME_OR_PASSWORD = 8
}