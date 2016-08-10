package models.lottery.drawing

import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._
import java.sql.Connection
import models.lottery.prize.Prize

case class DrawingPrize(id: Pk[Long], prize: Option[Prize], finalDigits: Option[TicketDigit])

object DrawingPrize {

  private val sql = """ SELECT *
                        FROM dbase_prize dbp, dbase db, drawing d, dbase_prizetype dbpt,
                        drawing_prize dp
                        LEFT OUTER JOIN drawing_finaldigit fd ON ( fd.drawing_prize_id = dp.id)
                        WHERE dp.drawing_id   = d.id
                        AND dp.prize_id       = dbp.id
                        AND db.id             = dbp.drawing_base_id
                        AND dbp.typ           = dbpt.id
                        AND d.id = {id} """

  private val order = " order by sort"

  def delete(id: Long) = {
    DB.withTransaction(implicit con => {
      val rows = SQL("delete from drawing_prize where id = {id}").onParams(id).executeUpdate()
      0 < rows
    })
  }

  def add(dprize: DrawingPrize, did: Long): Option[java.math.BigInteger] = {DB.withTransaction(implicit con => add(con, dprize, did))}

  def add(implicit connection: Connection, dprize: DrawingPrize, did: Long) = {
    var newIdOpt: Option[java.math.BigInteger] = None
    dprize.prize.map {
      prize =>
        SQL("insert into drawing_prize ( drawing_id, prize_id ) values ( {drawing_date}, {prize_id} )")
          .onParams(did, prize.id.get).executeUpdate()
        newIdOpt = SQL("SELECT last_insert_id()").as(scalar[java.math.BigInteger].singleOpt)
        newIdOpt.map(newDrawingPrizeId => dprize.finalDigits.map(TicketDigit.add(connection, newDrawingPrizeId.longValue(), _)))
    }
    newIdOpt
  }

  def byDrawing(did: Long): Seq[models.lottery.drawing.DrawingPrize] = DB.withConnection(implicit con =>
    SQL(sql + order).onParams(did).as(
      DrawingPrize.getInstance ~ (TicketDigit.getInstance ?) map {
        case dp ~ fd =>
          dp.copy(finalDigits = fd)
      } *))

  private val instance = get[Pk[Long]]("drawing_prize.id") ~ get[Long]("drawing_prize.drawing_id") ~ get[Long]("drawing_prize.prize_id")

  protected[lottery] def getInstance = instance map {
    case id ~ did ~ pid =>
      DrawingPrize(id, Prize.byId(pid), None)
  }
}