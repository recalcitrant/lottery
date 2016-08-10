package models.lottery.drawing

import anorm.SqlParser._
import anorm._
import java.sql.Connection
import play.api.db._
import play.api.Play.current
import annotation.tailrec
import scala.math.pow
import models.client.{UserQuery, QueryTicketRange}
import models.lottery.prize.PrizeType
import java.util.Date

case class TicketDigit(id: Pk[Long], digits: String, dateWinningNotification: Option[Date])

object TicketDigit {

	val stripLeadingZeros = "\\b0*([1-9][0-9]*|0)\\b".r

	def byDrawingAndTicketRanges(fdlist: List[(String, DrawingResult)], query: UserQuery) = {

		@tailrec
		def mergeRanges(rs: List[QueryTicketRange], sep: List[QueryTicketRange] = Nil): List[QueryTicketRange] = rs match {
			case x :: y :: rest =>
				if (y.fromNum > x.toNum) mergeRanges(y :: rest, x :: sep)
				else mergeRanges(QueryTicketRange(x.fromStr, (x.toNum max y.toNum).toString, x.fromNum, x.toNum max y.toNum) :: rest, sep)
			case _ => (rs ::: sep).reverse
		}

		def cashResultByDigits(res: Seq[DrawingResult]) = {
			res.groupBy(_.digits).map {
				case (_, drs) =>
					val head = drs.head
					DrawingResult(head.pid, drs.map(_.amount.toLong).sum.toString, head.pType, head.pTitle, head.ticketNum, head.digits, DrawingResult.TYPE_DIGITS)
			}
		}
		def matResultByPrizeAndDigits(res: Seq[DrawingResult]) = res.groupBy(_.pid).flatMap {
			case (_, seq) =>
				seq.groupBy(_.digits).map {
					case (no, drs) =>
						val head = drs.head
						DrawingResult(head.pid, drs.map(_.amount.toLong).sum.toString, head.pType, head.pTitle, head.ticketNum, head.digits, DrawingResult.TYPE_DIGITS, drs.size)
				}
		}
		val ranges = mergeRanges(query.ranges.flatten.toList.sortBy(_.fromNum))
		val result = new collection.mutable.ListBuffer[DrawingResult]
		for ((digits, dresult) <- fdlist) {
			val ten_pow_length_digits = pow(10, digits.length)
			val digitsWithoutLeadingZeros = (stripLeadingZeros replaceAllIn(digits, "$1")).toLong
			for (range <- ranges) {
				val loopstart = (range.fromNum / ten_pow_length_digits).toLong
				val loopend = (range.toNum / ten_pow_length_digits).toLong
				val fracstart = (range.fromNum - loopstart * ten_pow_length_digits).toLong
				val fracend = (range.toNum - loopend * ten_pow_length_digits).toLong
				for (i <- loopstart to loopend) {
					if ((i != loopstart || fracstart <= digitsWithoutLeadingZeros) &&
						(i != loopend || fracend >= digitsWithoutLeadingZeros)) {
						val combinedDigits = i + digits
						val stripped = (stripLeadingZeros replaceAllIn(combinedDigits, "$1")).toLong
						result += DrawingResult(dresult.pid, dresult.amount, dresult.pType, dresult.pTitle, stripped, TicketDigit(NotAssigned, digits, dresult.digits.dateWinningNotification), DrawingResult.TYPE_DIGITS)
					}
				}
			}
		}
		val (cashlist, matlist) = result.partition(PrizeType.CASH == _.pType)
		cashResultByDigits(cashlist) ++ matResultByPrizeAndDigits(matlist)
	}

	def add(implicit connection: Connection, dpid: Long, fd: TicketDigit) =
		SQL("insert into drawing_finaldigit ( drawing_prize_id, digits, date_winning_notification) values ( {drawing_prize_id}, {digits}, {date_winning_notification})").onParams(dpid, fd.digits, fd.dateWinningNotification.getOrElse(None)).executeUpdate()

	def add(pid: Long, did: Long, fd: TicketDigit): Int =
		DB.withConnection(implicit con => add(con, pid, fd))

	def update(implicit connection: Connection, dgts: (Long, String, Option[Date])) = {
		val dpid = dgts._1
		val digits = dgts._2
		val dateWinningNotification = dgts._3
		val optExists = SQL("select id from drawing_finaldigit where drawing_prize_id = {dpid}").onParams(dpid).as(scalar[Long].singleOpt)
		optExists.map(rows => {
			if ("" == digits) SQL("delete from drawing_finaldigit where drawing_prize_id = {dpid}").onParams(dpid).executeUpdate()
			else {
				SQL("update drawing_finaldigit set digits = {digits}, date_winning_notification = {date_winning_notification} where drawing_prize_id = {dpid}").onParams(digits, dateWinningNotification.getOrElse(None), dpid).executeUpdate()
			}
		}).getOrElse(if ("" != digits) SQL("insert into drawing_finaldigit ( drawing_prize_id, digits, date_winning_notification ) values ( {drawing_prize_id}, {digits}, {date_winning_notification} )").onParams(dpid, digits, dateWinningNotification.getOrElse(None)).executeUpdate())
	}

	protected[lottery] def getInstance = instance map {
		case fdid ~ digits ~ dateWinningNotification => TicketDigit(fdid, digits, dateWinningNotification)
	}

	private def instance = get[Pk[Long]]("drawing_finaldigit.id") ~ get[String]("drawing_finaldigit.digits") ~ get[Option[Date]]("drawing_finaldigit.date_winning_notification")
}