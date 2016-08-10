package models.lottery.ticket

import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._
import java.sql.Connection
import play.api.mvc.MultipartFormData
import play.api.libs.Files
import models.lottery.drawing.Drawing

case class TicketUpload(id: Pk[Long], did: Long, number: Long, amount: String)

object TicketUpload {

  val sql = " select * from dbase_prizeupload pu "

  def hasUploads(did: Long) = {
    DB.withConnection(implicit con =>
      0 < SQL("select count(id) from drawing_ticket where drawing_id = {did}").on("did" -> did).as(scalar[Long].single))
  }

  def listByDrawing(did: Long) = {
    DB.withConnection(implicit con =>
      Option(SQL("select * from drawing_ticket where drawing_id = {did}").on("did" -> did).as(instance *)))
  }

  def delete(implicit connection: Connection, did: Long) =
    SQL("delete from drawing_ticket where drawing_id = {id}").onParams(did).executeUpdate()

  def add(drawing: Drawing, fp: MultipartFormData.FilePart[Files.TemporaryFile]) = DB.withTransaction(implicit con => {
    TicketParser.parse(fp.ref.file, drawing) match {
      case Right(tickets) =>
        SQL("delete from drawing_ticket where drawing_id = {did}").onParams(drawing.id.get).executeUpdate()
        tickets.foreach {
          result =>
            SQL("insert into drawing_ticket(drawing_id, number, amount) values ( {did}, {number}, {amount} )").
              onParams(drawing.id.get, result._1, result._2).executeUpdate()
        }
        Right(true)
      case exc@Left(ex) => exc
    }
  })

  private val instance = get[Pk[Long]]("drawing_ticket.id") ~ get[Long]("drawing_ticket.drawing_id") ~
    get[Long]("drawing_ticket.number") ~ get[String]("drawing_ticket.amount") map {
    case id ~ did ~ number ~ amount => TicketUpload(id, did, number, amount)
  }
}