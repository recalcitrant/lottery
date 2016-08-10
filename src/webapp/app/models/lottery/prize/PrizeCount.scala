package models.lottery.prize

import play.api.db._
import anorm.SqlParser._
import anorm._
import java.sql.Connection
import play.api.Play.current

case class PrizeCount(did: Long, pid: Long, prizeType: Option[Long], title: Option[String], value: Option[String], times: Option[Long]) {

  def total = times.map(_ * value.get.toLong).getOrElse(0)

  def hasPositiveValue = value.exists(_ != "000")
}

object PrizeCount {

  def apply(did: Long, pid: Long, times: Option[Long]) = new PrizeCount(did, pid, None, None, None, times)

  private val sql = """SELECT distinct prize_id, dp.drawing_id, prize_id, typ, title, value, times
               FROM   dbase_prize dbp, drawing_prize dp
               LEFT OUTER JOIN drawing_prizecount dpc ON (dpc.drawing_id = dp.drawing_id AND dpc.dbase_prize_id = dp.prize_id)
               WHERE  dp.drawing_id = {did}
               AND    dp.prize_id = dbp.id
               ORDER  BY sort"""

  def byDrawing(did: Long) = DB.withConnection(implicit con =>
    SQL(sql).on("did" -> did).as(PrizeCount.instance *))

  def addAll(implicit connection: Connection, pcounts: Seq[PrizeCount]): Option[Boolean] = {
    pcounts.foreach(update(connection, _))
    Some(true)
  }

  def update(implicit connection: Connection, pcount: PrizeCount) = {
    val optExists = SQL("select id from drawing_prizecount where drawing_id = {did} and dbase_prize_id = {pid}").onParams(pcount.did, pcount.pid).as(scalar[Long].singleOpt)
    optExists.map(rows => {
      pcount.times.map(times => {
        if (0 == times) SQL("delete from drawing_prizecount where drawing_id = {did} and dbase_prize_id = {pid} ").onParams(pcount.did, pcount.pid).executeUpdate()
        else SQL("update drawing_prizecount set times = {times} where drawing_id = {did} and dbase_prize_id = {pid}").onParams(times, pcount.did, pcount.pid).executeUpdate()
      })
    }).getOrElse(add(connection, PrizeCount(pcount.did, pcount.pid, pcount.times)))
  }

  private def add(implicit connection: Connection, pc: PrizeCount) =
    SQL("insert into drawing_prizecount ( drawing_id, dbase_prize_id, times ) values ( {drawing_id}, {dbase_prize_id}, {times} )").onParams(pc.did, pc.pid, pc.times).executeUpdate()

  private def instance = getInstance.map {
    case did ~ pid ~ title ~ value ~ ptype ~ times =>
      PrizeCount(did, pid, Option(ptype), title, Option(value), times)
  }

  private val getInstance = get[Long]("drawing_prize.drawing_id") ~ get[Long]("drawing_prize.prize_id") ~
    get[Option[String]]("dbase_prize.title") ~ get[String]("dbase_prize.value") ~ get[Long]("dbase_prize.typ") ~ get[Option[Long]]("drawing_prizecount.times")
}