package utils.mail

import exception.{ExceptionCodes, AppException}
import scala.collection.JavaConversions._
import javax.mail.internet.InternetAddress
import views.txt
import models.lottery.drawing.Drawing
import play.api.{Play, Logger}
import play.api.Play.current
import org.apache.commons.mail._
import models.client.{NotifiedWinner, Client}
import models.lottery.Lottery
import utils.Config
import utils.Format
import MailTemplateFunctions._

object Mailer {

	def clientWinningNotification(winner: NotifiedWinner) = {
		try {
			val email = new HtmlEmail
			val mailConfig = winner.mailConfig
			email.setHostName(mailConfig.server)
			email.setSmtpPort(mailConfig.port)
			email.setAuthenticator(new DefaultAuthenticator(mailConfig.user, mailConfig.pw))
			email.setFrom(mailConfig.fromAddress)
			email.setCharset("utf-8")
			val subject = winner.lotteryName + " - Gewinnbenachrichtigung"
			email.setSubject(subject)
			email.setTextMsg(winner.body.replaceFirst("\n", ""))
			winner.bodyHtml.map(email.setHtmlMsg)
			email.addTo(winner.recipient)
			Right(email.send())
		} catch {
			case ex: Throwable =>
				val msg = AppException(ExceptionCodes.EMAIL_NOT_SENT, Option(ex))
				Logger.error("Mailer.clientWinningNotification " + msg.toString)
				Left(msg)
		}
	}

	def userLockedOut(username: String, recipient: String) = {
		val email = instance
		val subject = "Neues Passwort für " + username
		val body = txt.email.user_locked_out(username).body
		email.setSubject(subject)
		email.setMsg(body.replaceFirst("\n", ""))
		email.addTo(recipient)
		send(email)
	}

	def clientRegister(lottery: Lottery, client: Client, content: String, pw: String) {
		val email = instance
		val subject = lottery.lotteryName + " - Anmeldung zur Gewinnbenachrichtigung"
		val body = clientRegisterTemplates.getOrElse(lottery.templateSuffix, views.txt.email.client_register.f)(client, content, pw).body
		email.setSubject(subject)
		email.addTo(client.email.getOrElse("undefined"))
		email.setMsg(body.replaceFirst("\n", ""))
		email.send()
	}

	def clientUpdate(lottery: Lottery, client: Client, content: String, pwChanged: Boolean) = {
		val email = instance
		val subject = lottery.lotteryName + " - Änderung Ihrer Gewinnbenachrichtigung"
		val body = clientUpdateTemplates.getOrElse(lottery.templateSuffix, views.txt.email.client_update.f)(client, content, pwChanged.asInstanceOf[java.lang.Boolean]).body
		email.setSubject(subject)
		email.setMsg(body.replaceFirst("\n", ""))
		email.addTo(client.email.getOrElse("undefined"))
		send(email)
	}

	def clientNewPassword(lottery: Lottery, client: Client, pw: String) = {
		val email = instance
		val subject = lottery.lotteryName + " - Neues Passwort"
		val body = clientNewPasswordTemplates.getOrElse(lottery.templateSuffix, views.txt.email.client_new_password.f)(client, pw).body
		email.setSubject(subject)
		email.setMsg(body.replaceFirst("\n", ""))
		email.addTo(client.email.getOrElse("undefined"))
		send(email)
	}

	def clientUnregister(lottery: Lottery, client: Client) = {
		val email = instance
		val subject = lottery.lotteryName + " - Löschbestätigung Ihrer Gewinnbenachrichtigung"
		val body = clientUnregisterTemplates.getOrElse(lottery.templateSuffix, views.txt.email.client_unregister.f)(client).body
		email.setSubject(subject)
		email.setMsg(body.replaceFirst("\n", ""))
		email.addTo(client.email.getOrElse("undefined"))
		send(email)
	}

	def workflowAction(drw: Option[Drawing], state: String, comment: String, recipients: Seq[InternetAddress]) = {
		drw.map {
			drawing =>
				val email = instance
				val subject = drawing.dbase.lottery.nameshort + " - " + drawing.dbase.drawingType.name + " " + drawing.date.fold("unbekannt")(Format.date)
				val body = txt.email.workflow_action(comment, state).body
				email.setSubject(subject)
				email.setMsg(body.replaceFirst("\n", ""))
				email.setTo(recipients)
				send(email)
		}
	}

	private def send(email: Email) = {
		try {
			Right(email.send())
		}
		catch {
			case ex: Throwable =>
				val msg = AppException(ExceptionCodes.EMAIL_NOT_SENT, Option(ex))
				Logger.error(msg.toString)
				Left(msg)
		}
	}

	private def instance = getMail(new SimpleEmail())

	private def getMail(email: Email) = {
		email.setHostName(Config.getString("smtp.host"))
		email.setSmtpPort(Config.getString("smtp.port").toInt)
		email.setAuthenticator(new DefaultAuthenticator(Config.getString("smtp.user"), Config.getString("smtp.pass")))
		email.setFrom(Config.getString("smtp.from"))
		// for testing purposes:
		if (Play.isDev) {
			email.setSSLOnConnect(true)
			email.setSslSmtpPort(Config.getString("smtp.port"))
		}
		email.setCharset("utf-8")
		email
	}
}
