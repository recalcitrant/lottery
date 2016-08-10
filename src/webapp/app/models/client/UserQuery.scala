package models.client

import anorm._
import anorm.SqlParser._
import utils.NumberUtils.str2Long
import java.sql.Connection

case class UserQuery(ranges: Seq[Option[QueryTicketRange]])

case class QueryTicketRange(fromStr: String, toStr: String, fromNum: Long, toNum: Long)

object UserQuery {

	def add(implicit connection: Connection, cid: Long, query: UserQuery) = {
		val back = try {
			query.ranges.foreach(range => range.map {
				r => SQL("insert into client_ticket ( client_id, start, end ) values ( {id},{start},{end} )").onParams(cid, r.fromStr, r.toStr).executeUpdate()
			})
			Right(true)
		} catch {
			case ex: Throwable => Left(false)
		}
		back
	}

	def update(implicit connection: Connection, cid: Long, query: UserQuery) = {
		val back = try {
			SQL("delete from client_ticket where client_id = {cid}").onParams(cid).executeUpdate()
			query.ranges.foreach(range => range.map {
				r => SQL("insert into client_ticket ( client_id, start, end ) values ( {id},{start},{end} )").onParams(cid, r.fromStr, r.toStr).executeUpdate()
			})
			Right(true)
		} catch {
			case ex: Throwable => Left(false)
		}
		back
	}
}

object QueryTicketRange {

	private val stripLeadingZeros = "\\b0*([1-9][0-9]*|0)\\b".r

	private[client] def instance = {
		get[Option[String]]("client_ticket.start") ~ get[Option[String]]("client_ticket.end") map {
			case from ~ to => {
				if (from.isDefined && to.isDefined) Option(QueryTicketRange(from.get, to.get, str2Long(stripLeadingZeros replaceAllIn(from.get, "$1")), str2Long(stripLeadingZeros replaceAllIn(to.get, "$1"))))
				else None
			}
		}
	}
}
