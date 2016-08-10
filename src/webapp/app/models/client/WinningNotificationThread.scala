package models.client

import akka.actor._
import concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.Config
import exception.AppException
import utils.mail.Mailer

case object Dispatch

object WinningNotificationThread {

	def start() {
		ActorSystem("WinningNotificationDispatcher").actorOf(Props[MailDispatcher])
	}
}

class MailDispatcher extends Actor with ActorLogging {

	private val ticker = context.system.scheduler.schedule(
		10 seconds,
		Config.getLong("play.akka.winningnotification.mail.check.interval") minutes,
		self,
		Dispatch)

	def receive = {
		case Dispatch => dispatch()
		case e: Exception => log.error("MailDispatcher got unexpected Message: " + e)
	}

	def dispatch() {
		NotifiedWinner.list.foreach {
			winner =>
				Mailer.clientWinningNotification(winner) match {
					case Right(retval) => postNotification(winner)
					case Left(ex: AppException) => NotifiedWinner.incrementNotificationAttempts(winner.id.get)
				}
		}
	}

	private def postNotification(winner: NotifiedWinner) {
		NotificationHistory.recordHistory(winner)
		NotifiedWinner.deleteFromQueue(winner.id.get)
	}

	override def postStop() {
		ticker.cancel()
	}

	override def preRestart(reason: Throwable, msg: Option[Any]) {
		log.error("MailDispatcher preRestart : " + reason.printStackTrace() + " " + msg.toString)
	}

	override def postRestart(reason: Throwable) {
		log.error("MailDispatcher postRestart OK")
	}
}