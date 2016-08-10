package models.lottery.ticket

import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._

import models.lottery.drawing.{TicketDigit, DrawingResult}
import models.client.UserQuery

case class Ticket(id: Pk[Long], number: Long, amount: String)

object Ticket {

  val sql = "select * from drawing_ticket where drawing_id = {did}"

  def byDrawing(did: Long) = DB.withConnection(implicit con =>
    SQL(sql).onParams(did).as(instance *))

  def byDrawingAndTicketRanges(did: Long, query: UserQuery) =
    DB.withConnection(implicit con => {
      val ranges = query.ranges.flatten
      val rsql = ranges.map {
        range =>
          "( number >= " + range.fromNum + " AND number <= " + range.toNum + " )"
      }.mkString(" OR ")
      val finSql = sql + (if (ranges.nonEmpty) " AND " + "(" + rsql + ")")
      if (ranges.isEmpty) Nil
      else SQL(finSql).onParams(did).as(publicInstance *)
    })

  // note that Tickets can only carry a cash-prize, therefore NO prize-id:
  private def publicInstance = get[Pk[Long]]("drawing_ticket.id") ~ get[Long]("drawing_ticket.number") ~ get[String]("drawing_ticket.amount") map {
    case id ~ number ~ amount => DrawingResult(-1, amount, 1, None, number, TicketDigit(NotAssigned, number.toString, None), DrawingResult.TYPE_TICKET)
  }

  private def instance = get[Pk[Long]]("drawing_ticket.id") ~ get[Long]("drawing_ticket.number") ~ get[String]("drawing_ticket.amount") map {
    case id ~ number ~ amount => Ticket(id, number, amount)
  }
}