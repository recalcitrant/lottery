package models.lottery.prize

import _root_.utils.Format
import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._
import java.sql.Connection

case class Prize(id: Pk[Long], value: String, title: Option[String], description: Option[String], prizeType: PrizeType, sort: Long, uploads: Seq[PrizeUpload] = Seq[PrizeUpload]()) {

  def display =
    if (prizeType.id.get == PrizeType.CASH) Format.currency(value)
    else title.getOrElse("Keine Angabe")
}

object Prize {

  val CASH = 1
  val MATERIAL = 2
  private val sql_by_id = " select * from dbase_prize p, dbase_prizetype pt where p.typ = pt.id and p.id = {id}"

  def getDescription(pid: Long) = DB.withConnection(implicit con => SQL("SELECT description from dbase_prize p where p.id = {id}")
    .onParams(pid).as(scalar[Option[String]].single))

  def getTitle(pid: Long) = DB.withConnection(implicit con => SQL("SELECT title from dbase_prize p where p.id = {id}")
    .onParams(pid).as(scalar[Option[String]].single))

  def updateDescription(id: Long, desc: String) = {
    DB.withConnection {
      implicit con =>
        val rows = SQL("update dbase_prize set description = {description} where id = {id}")
          .onParams(desc, id).executeUpdate()
        if (0 == rows) None
        else Some(rows)
    }
  }

  def updateTitle(id: Long, title: String) = {
    DB.withConnection {
      implicit con =>
        val rows = SQL("update dbase_prize set title = {title} where id = {id}")
          .onParams(title, id).executeUpdate()
        if (0 == rows) None
        else Some(rows)
    }
  }

  def add(implicit connection: Connection, dbid: Long, prize: Prize) = {
    val rows = SQL("insert into dbase_prize(drawing_base_id, typ, value, sort) values ( {dbid}, {typ}, {value}, {sort} )")
      .onParams(dbid, prize.prizeType.id.get, prize.value, prize.sort).executeUpdate()
    if (0 == rows) None else Some(rows)
  }

  def update(implicit connection: Connection, lid: Long, prize: Prize) {
    val oldtype = SQL("SELECT typ from dbase_prize where id = {id}").onParams(prize.id.get).as(scalar[Long].single)
    val newtype = prize.prizeType.id.get
    // did we switch from material- to cash-prize? if so delete prize-uploads, title and description:
    if (PrizeType.CASH == newtype && PrizeType.MATERIAL == oldtype) {
      PrizeUpload.deleteByPrize(connection, lid, prize.id.get)
      SQL("update dbase_prize set description = '' where id = {id}").onParams(prize.id.get).executeUpdate()
      SQL("update dbase_prize set title = '' where id = {id}").onParams(prize.id.get).executeUpdate()
    }
    val rows = SQL("update dbase_prize set typ = {typ}, value = {value}, sort = {sort} where id = {id}")
      .onParams(prize.prizeType.id.get, prize.value, prize.sort, prize.id.get).executeUpdate()
    if (0 == rows) None
    else Some(rows)
  }

  def addViaTransaction(dbid: Long, prize: Prize) = {
    DB.withTransaction {
      implicit con =>

        val rows = SQL("insert into dbase_prize(drawing_base_id, typ, value, sort) values ( {dbid}, {typ}, {value}, {sort} )")
          .onParams(dbid, prize.prizeType.id.get, prize.value, prize.sort).executeUpdate()

        if (0 == rows) None
        else Some(SQL("SELECT last_insert_id()").as(scalar[java.math.BigInteger].single))
    }
  }

  def byId(id: Long): Option[Prize] = DB.withConnection(implicit con =>
    SQL(sql_by_id).on("id" -> id).as(Prize.getInstance.singleOpt))

  def delete(id: Long, dbid: Long) = {
    DB.withTransaction {
      implicit con =>
      // does a drawing exist? if so deny prize-removal:
        SQL("select count(d.id) from drawing d, dbase db where d.drawing_base_id = db.id and db.id = {id}").onParams(dbid).as(scalar[Long].singleOpt).exists(count =>
	        if (0 == count) {
		        SQL("delete from dbase_prize where id = {id}").onParams(id).executeUpdate()
		        true
	        } else false)
    }
  }

  /*def deleteByDrawingBase(dbid: Long) = DB.withConnection(implicit con =>
    SQL("delete from prize where drawing_base_id = {dbid}").onParams(dbid).executeUpdate())*/

  private val instance = get[Pk[Long]]("dbase_prize.id") ~ get[String]("dbase_prize.value") ~ get[Option[String]]("dbase_prize.description") ~ get[Option[String]]("dbase_prize.title") ~ get[Pk[Long]]("dbase_prizetype.id") ~ get[String]("dbase_prizetype.name") ~ get[Int]("dbase_prize.sort")

  protected[lottery] def getInstance = instance map {
    case id ~ value ~ desc ~ title ~ typeid ~ name ~ sort =>
      Prize(id, value, title, desc, PrizeType(typeid, name), sort, PrizeUpload.byPrizeId(id.get))
  }
}