package models.client

import java.util.{Date, Calendar}
import anorm._
import models.lottery.drawing.{WinningNotification, DrawingResult, Drawing}
import models.lottery.prize.PrizeType
import play.Logger
import play.api.db.DB
import utils.Format
import play.api.Play.current

object WinningNotifictionCreator {

	def addToWinningNotificationQueue(clients: Seq[Client], drawing: Drawing) = {

		val lottery = drawing.dbase.lottery
		val mailConf = lottery.notificationConfig

		val templateMapTxt = Map(
			"hes" -> views.txt.email.client_winning_notification_hes.f,
			"osv" -> views.txt.email.client_winning_notification_osb.f,
			"rsg" -> views.txt.email.client_winning_notification_rsg.f,
			"svn" -> views.txt.email.client_winning_notification_svn.f,
			"thu" -> views.txt.email.client_winning_notification_thu.f,
			"wes" -> views.txt.email.client_winning_notification_wes.f,
			"wtm" -> views.txt.email.client_winning_notification_wtm.f
		)
		val templateMapHtml = Map(
			"hes" -> views.html.email.client_winning_notification_hes.f,
			"thu" -> views.html.email.client_winning_notification_thu.f,
			"osv" -> views.html.email.client_winning_notification_osb.f,
			"rsg" -> views.html.email.client_winning_notification_rsg.f,
			"svn" -> views.html.email.client_winning_notification_svn.f,
			"wes" -> views.html.email.client_winning_notification_wes.f,
			"wtm" -> views.html.email.client_winning_notification_wtm.f
		)
		try {
			clients.foreach {
				client => {
					client.query.map {
						query =>
							val resultList = Drawing.userQuery(drawing.id.get, lottery.lotteryType.id.get, query)
							// do not send emails to clients that have NOT won:
							if (resultList.nonEmpty) {
								// check if notificationDates were set in prize categories:
								val notificationDates = resultList.flatMap(_.digits.dateWinningNotification)
								val (textTxt, textHtml, total) = resultList.foldLeft((new String, new String, 0l)) {
									(tuple, drawingResult) =>
										(tuple._1 + textOrHtml(drawingResult, isHtml = false),
											tuple._2 + textOrHtml(drawingResult, isHtml = true),
											tuple._3 + drawingResult.amount.toLong)
								}
								val funcTxt = templateMapTxt.getOrElse(lottery.templateSuffix, views.txt.email.client_winning_notification.f)
								val totalStringTxt = textOrHtmlTotal(total, isHtml = false)
								val totalStringHtml = textOrHtmlTotal(total, isHtml = true)
								val prizesWonTxt = textTxt.toString.substring(0, textTxt.toString.length - 1) + (if ("" != totalStringTxt) "\n" + totalStringTxt else "")
								val prizesWonHtml = textHtml.toString + (if ("" != totalStringHtml) totalStringHtml else "")
								val lotteryUrl = if (winningNotificationContentIsEmpty(drawing.winningNotificationContent)) "<" + client.branch.url + client.branch.loginUrl + ">"
								else "<a href=\"" + client.branch.url + client.branch.loginUrl + "\">" + client.branch.url + "</a>"
								val bodyTxt = funcTxt(client, prizesWonTxt, drawing.date.fold("")(Format.date), drawing.dbase.drawingType.name, lottery.lotteryName, lotteryUrl).body
								val bodyHtml = drawing.winningNotificationContent.map { content =>
									if (content.desc.isEmpty || content.url.isEmpty) None
									else {
										val teaserText = content.desc.map(_.replaceAll("\n", "<br/>"))
										val teaserHtml = views.html.email.client_winning_notification_teaser(teaserText, content.url, drawing.dbase.lottery.id.get, drawing.id.get).body
										val funcHtml = templateMapHtml.getOrElse(lottery.templateSuffix, views.html.email.client_winning_notification.f)
										Some(funcHtml(client, prizesWonHtml, drawing.date.fold("")(Format.date), drawing.dbase.drawingType.name, lottery.lotteryName, lotteryUrl, teaserHtml).body)
									}
								} getOrElse None
								// check if notificationDates were set in prize categories, if YES use the earliest date if NOT use the drawing's notification-date:
								val actualNotificationDate = if (notificationDates.nonEmpty) notificationDates.min else drawing.dateWinningNotification.getOrElse(defaultWinningNotificationDate)
								DB.withConnection(implicit con => {
									SQL("insert into client_tmp_mail (did, lottery_name, firstname, lastname, amount, recipient, body, body_html, mail_server, mail_port, mail_user, mail_password, mail_tls, mail_from_address, date_winning_notification) values ( {did}, {lname}, {firstname}, {lastname}, {amount}, {recipient}, {body}, {body_html}, {mail_server}, {mail_port}, {mail_user}, {mail_pass}, {mail_tls}, {mail_from_address}, {date_winning_notification} )").
										onParams(drawing.id.get, lottery.lotteryName, client.firstName, client.lastName, total.toString, client.email, bodyTxt, bodyHtml, mailConf.server, mailConf.port, mailConf.user, mailConf.pw, if (mailConf.tls) 1 else 0, mailConf.fromAddress, actualNotificationDate).executeUpdate()
								})
							}
					}
				}
			}
		} catch {
			case ex: Throwable => Logger.error("Client.addToWinningNotificationQueue " + ex)
		}
	}

	def winningNotificationContentIsEmpty(content: Option[WinningNotification]) = {
		content.isEmpty || content.exists(c => c.desc.isEmpty || c.url.isEmpty)
	}

	def textOrHtml(drawingResult: DrawingResult, isHtml: Boolean) = {
		(if (isHtml) "<strong>" else "") +
			(if (DrawingResult.TYPE_DIGITS == drawingResult.resultType) "Endziffer " else "Losnummer ") + drawingResult.digits.digits + ": " +
			(if (PrizeType.CASH == drawingResult.pType) Format.currency(drawingResult.amount)
			else new String + drawingResult.prizeCount + " x " + drawingResult.pTitle.fold("")(_ + (if ("0" == drawingResult.amount) "" else " im " + (if (1 == drawingResult.prizeCount) "Wert" else "Gesamtwert") + " von " + Format.currency(drawingResult.amount)))) +
			(if (isHtml) "</strong><br/>" else "\n")
	}

	def textOrHtmlTotal(total: Long, isHtml: Boolean) = {
		if (0 != total) {
			(if (isHtml) "<br/><strong>" else "\n") +
				"Gesamtgewinn: " + Format.currency(total.toString) +
				(if (isHtml) "</strong>" else "")
		} else ""
	}

	def defaultWinningNotificationDate = {
		// if no "notification_date" was given add one hour to "NOW"
		val cal = Calendar.getInstance()
		cal.setTime(new Date())
		cal.add(Calendar.HOUR_OF_DAY, 1)
		val oneHourAhead = cal.getTime
		oneHourAhead
	}
}
