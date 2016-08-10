package models.lottery.drawing

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.util.Date
import models.workflow.Workflow
import models.auth.User
import models.lottery.dbase.DrawingBase
import models.lottery.drawing.DrawingSQL._
import models.lottery.prize.PrizeCount
import models.lottery.LotteryType
import models.lottery.ticket.{TicketUpload, Ticket}
import models.client.{NotificationHistory, UserQuery}
import utils.db.BlockingJDBC
import org.joda.time.{Months, DateTime}

case class Drawing(id: Pk[Long],
                   dbase: DrawingBase,
                   prizes: Seq[DrawingPrize],
                   prizeCount: Option[Seq[PrizeCount]],
                   tickets: Option[Seq[Ticket]],
                   workflow: Option[Workflow],
                   winningNotificationContent: Option[WinningNotification],
                   date: Option[Date],
                   dateNext: Option[Date],
                   datePublishNext: Option[Date],
                   dateWinningNotification: Option[Date]) {

	def countTable = prizeCount.map {
		_.filter(_.times.isDefined)
	}.getOrElse(Nil)

	def totalCount = prizeCount.fold(0l)(_.foldLeft(0l) {
		(acc, pc) =>
			acc + pc.times.getOrElse(0l)
	})

	def totalAmount = prizeCount.fold("0")(_.foldLeft(0l) {
		(acc, pc) =>
			acc + pc.value.getOrElse("0").toLong * pc.times.getOrElse(0l)
	}.toString)

	def hasTicketUploads = TicketUpload.hasUploads(id.get)

	def hasNotificationHistory = NotificationHistory.hasHistory(id.get)
}

case class DrawingResult(pid: Long,
                         amount: String,
                         pType: Long,
                         pTitle: Option[String],
                         ticketNum: Long,
                         digits: TicketDigit,
                         resultType: String,
                         prizeCount: Int = 1)

object DrawingResult {
	val TYPE_TICKET = "ticket"
	val TYPE_DIGITS = "digits"
}

object Drawing extends BlockingJDBC {

	def listLatestByBranch(bcode: String) =
		sqlWithFuture { implicit con =>
			val threshold = new DateTime(2013, 10, 1, 0, 0).withTimeAtStartOfDay
			val lid = models.lottery.Lottery.IdbyBranchCode(bcode).getOrElse(-1l)
			val visible = models.lottery.Lottery.getDrawingsVisible(lid).getOrElse(6)
			val newList = SQL(sqlByBranch).onParams(bcode, visible).as(publicInstance *)
			// Starting October 2013 WHVN belongs to SVN so combine new SVN-drawings with old WHVN-drawings for customers to see:
			// => 106 28250110
			val whvn = "28250110"
			if (bcode == whvn) {
				val ddate = newList.head.date.map(d => new DateTime(d).withTimeAtStartOfDay()).get
				if (ddate.isEqual(threshold) || ddate.isAfter(threshold)) {
					val interval = Math.abs(Months.monthsBetween(threshold.withDayOfMonth(1), ddate.withDayOfMonth(1)).getMonths) + 1
					val oldList = SQL(sqlByOLD_WHVN).onParams(visible).as(publicInstance *)
					val finalList = (newList.take(interval) ::: oldList).take(visible)
					Option(finalList)
				} else {
					val whvnlid = 106
					val visible2 = models.lottery.Lottery.getDrawingsVisible(whvnlid).getOrElse(6)
					val oldList2 = SQL(sqlByOLD_WHVN).onParams(visible2).as(publicInstance *)
					Option(oldList2)
				}
			} else {
				Option(newList)
			}
		}

	def userQuery(did: Long, ltype: Long, query: UserQuery): List[DrawingResult] = DB.withConnection(implicit con => {
		val fdResult = TicketDigit.byDrawingAndTicketRanges(SQL(sqlPublicQuery).onParams(did).as(publicData *), query).toList
		if (LotteryType.EXTENDED == ltype) {
			val ticketResult = byDrawingAndTicketRanges(did, query)
			(ticketResult ++ fdResult).sortWith((a, b) => a.ticketNum < b.ticketNum)
		} else fdResult
	})

	def byId(id: Long, user: User) = DB.withConnection(implicit con =>
		SQL(sql + byid + order).on("id" -> id).as(instance(user).singleOpt))

	def publicById(id: Long) = DB.withConnection(implicit con =>
		SQL(sql + byid + order).on("id" -> id).as(publicInstance.singleOpt))

	def list(user: User) = DB.withConnection(implicit con =>
		SQL(sql + bylid + order).on("lid" -> user.lottery.id.get).as(instance(user) *))

	def update(id: Long, prizes: List[Option[(Long, String, Option[Date])]], date: Option[Date], date_next: Option[Date], date_publish_next: Option[Date], date_winning_notification: Option[Date]) = {
		DB.withTransaction {
			implicit con =>
				SQL(sqlUpdate).onParams(date.getOrElse(None), date_next.getOrElse(None), date_winning_notification.getOrElse(None), date_publish_next.getOrElse(None), id).executeUpdate()
				prizes.foreach(_.map(TicketDigit.update(con, _)))
		}
		// todo return adequate value
		Some(true)
	}

	def updatePrizeCountsAndTotalAmount(id: Long, pcounts: List[models.lottery.prize.PrizeCount]) = {
		DB.withTransaction(implicit con => {
			models.lottery.prize.PrizeCount.addAll(con, pcounts)
		})
		Some(true)
	}

	def delete(lid: Long, id: Long) = {
		DB.withTransaction {
			implicit con =>
				WinningNotification.deleteByDrawingId(lid, id)
				val rows = SQL(sqlDelete).onParams(id).executeUpdate()
				if (0 == rows) None
				else Some(rows)
		}
	}

	def add(d: Drawing) = {
		var newIdOpt: Option[java.math.BigInteger] = None
		DB.withTransaction {
			implicit con =>
				SQL(sqlInsert).onParams(d.date.getOrElse(None), d.dateNext.getOrElse(None), d.dateWinningNotification.getOrElse(None), d.datePublishNext.getOrElse(None), d.dbase.id.get).executeUpdate()
				newIdOpt = SQL("SELECT last_insert_id()").as(scalar[java.math.BigInteger].singleOpt)
				newIdOpt.map(newDrawingId => {
					SQL(sqlWorkflowInsert).onParams(newDrawingId.longValue(), Workflow.STATE_OFFEN).executeUpdate()
					for (dprize <- d.prizes) DrawingPrize.add(con, dprize, newDrawingId.longValue())
				})
		}
		newIdOpt
	}
}