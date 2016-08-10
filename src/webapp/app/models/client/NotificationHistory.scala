package models.client

import play.api.Play.current
import play.api.db._
import anorm.SqlParser._
import anorm._
import java.util.Date

case class NotificationHistory(id: Pk[Long],
                               did: Long,
                               firstname: String,
                               lastname: String,
                               recipient: String,
                               amount: String,
                               dateSent: Date)

object NotificationHistory {

  private val TIME_TO_ELAPSE_BEFORE_VISIBLE = 6

  def hasHistory(did: Long) = {
    DB.withConnection(implicit con =>
      0 < SQL("SELECT COUNT(id) FROM client_history_mail WHERE did = {did} and HOUR(TIMEDIFF(NOW(), date_sent)) >= {diff}").onParams(did, TIME_TO_ELAPSE_BEFORE_VISIBLE).as(scalar[Long].single))
  }

  def showHistory(did: Long) = DB.withConnection(implicit con =>
    Option(SQL("SELECT * FROM client_history_mail WHERE did = {did}").onParams(did).as(instance *)))

  def recordHistory(winner: NotifiedWinner) = {
    DB.withConnection {
      implicit con =>
        val rows = SQL("INSERT INTO client_history_mail (did, firstname, lastname, recipient, amount, date_sent) VALUES ({did}, {firstname}, {lastname}, {recipient}, {amount}, {date_sent})").
          onParams(winner.did, winner.firstname, winner.lastname, winner.recipient, winner.amount, new Date).executeUpdate()
        if (0 == rows) None
        else Some(rows)
    }
  }

  private val instance = get[Pk[Long]]("client_history_mail.id") ~ get[Long]("client_history_mail.did") ~
    get[String]("client_history_mail.firstname") ~ get[String]("client_history_mail.lastname") ~
    get[String]("client_history_mail.recipient") ~ get[String]("client_history_mail.amount") ~ get[Date]("client_history_mail.date_sent") map {
    case id ~ did ~ fname ~ lname ~ recipient ~ amount ~ sent => {
      NotificationHistory(id, did, fname, lname, recipient, amount, sent)
    }
  }
}