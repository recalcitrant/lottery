package models.client

import play.api.Play.current
import models.lottery.WinningNotificatonConfig
import play.api.db._
import anorm.SqlParser._
import anorm._
import java.util.Date

case class NotifiedWinner(id: Pk[Long],
                          lotteryName: String,
                          did: Long,
                          firstname: String,
                          lastname: String,
                          amount: String,
                          recipient: String,
                          body: String,
                          bodyHtml: Option[String],
                          attempts: Int,
                          notificationDate: Date,
                          mailConfig: WinningNotificatonConfig)

object NotifiedWinner {

  val MAX_NOTIFICATION_ATTEMPTS = 2

  def list = DB.withConnection(implicit con =>
    SQL("SELECT * FROM client_tmp_mail WHERE date_winning_notification <= NOW()").as(NotifiedWinner.instance *))

  def deleteFromQueue(id: Long) = {
    DB.withConnection {
      implicit con =>
        val rows = SQL("delete from client_tmp_mail where id = {id}").onParams(id).executeUpdate()
        if (0 == rows) None
        else Some(rows)
    }
  }

  def incrementNotificationAttempts(id: Long) {
    DB.withConnection {
      implicit con => {
        SQL("update client_tmp_mail set attempts = attempts + 1 where id = {id}").onParams(id).executeUpdate()
        if (maxNotificationAttemptsReached(id)) copyToFailedAttempts(id).map {
          copyOK => deleteFromQueue(id)
        }
      }
    }
  }

  private def copyToFailedAttempts(id: Long) = DB.withConnection {
    implicit con =>
      val rows = SQL("insert into client_failed_mail select * from client_tmp_mail where id = {id}").onParams(id).executeUpdate()
      if (0 == rows) None
      else Some(rows)
  }

  private def maxNotificationAttemptsReached(id: Long) = {
    DB.withConnection {
      implicit con =>
        MAX_NOTIFICATION_ATTEMPTS <= SQL("SELECT attempts from client_tmp_mail where id = {id}").onParams(id).as(scalar[Long].single)
    }
  }

  private val instance = get[Pk[Long]]("client_tmp_mail.id") ~ get[Long]("client_tmp_mail.did") ~ get[String]("client_tmp_mail.lottery_name") ~
    get[String]("client_tmp_mail.firstname") ~ get[String]("client_tmp_mail.lastname") ~ get[String]("client_tmp_mail.amount") ~
    get[String]("client_tmp_mail.recipient") ~ get[String]("client_tmp_mail.body") ~  get[Option[String]]("client_tmp_mail.body_html") ~
    get[Int]("client_tmp_mail.attempts") ~ get[Date]("client_tmp_mail.date_winning_notification") ~
    get[String]("client_tmp_mail.mail_server") ~ get[Int]("client_tmp_mail.mail_port") ~ get[String]("client_tmp_mail.mail_user") ~
    get[String]("client_tmp_mail.mail_password") ~ get[Int]("client_tmp_mail.mail_tls") ~ get[String]("client_tmp_mail.mail_from_address") map {
    case id ~ did ~ lotname ~ fname ~ lname ~ amount ~ recipient ~ body ~ bodyHtml ~ attempts ~ dt ~ server ~ port ~ user ~ pass ~ tls ~ from => {
      NotifiedWinner(id, lotname, did, fname, lname, amount, recipient, body, bodyHtml, attempts, dt, WinningNotificatonConfig(server, port, user, pass, (1 == tls), from))
    }
  }
}
