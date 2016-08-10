package models.lottery.dbase

import play.api.db.DB
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import models.lottery.drawing.DrawingType
import models.lottery.Lottery
import models.lottery.prize.{PrizeType, Prize}

case class DrawingBase(id: Pk[Long],
                       name: String,
                       lottery: Lottery,
                       drawingType: DrawingType,
                       prizes: Seq[Prize] = Seq[Prize]()) {

  // only offer to create drawings refering to this base if ALL material prizes have a title and description:
	def isComplete = 0 < prizes.size &&
    prizes.filter(PrizeType.MATERIAL == _.prizeType.id.get).forall(
      prize => prize.title.exists("" != _) && prize.description.exists("" != _))

}

object DrawingBase {

  private val sql = " select * from dbase db "
  private val join = " join dbase_prize p on db.id = p.drawing_base_id join dbase_prizetype pt on pt.id = p.typ "
  private val order = " order by db.typ, db.id desc"

  def add(lid: Long, dbname: String, typ: Long, prizes: Seq[Prize]) = {
    var newid: Option[java.math.BigInteger] = None
    DB.withTransaction {
      implicit con =>
        SQL("insert into dbase(name, lottery_id, typ) values ( {name}, {lottery_id}, {typ} )")
          .onParams(dbname, lid, typ).executeUpdate()
        newid = SQL("SELECT last_insert_id()").as(scalar[java.math.BigInteger].singleOpt)
        newid.map(did => prizes.foreach {Prize.add(con, did.longValue(), _)})
    }
    newid
  }

  def update(id: Long, lid: Long, name: String, prizes: Seq[Prize]) = {
    DB.withTransaction {
      implicit con =>
        SQL("update dbase set name = {name} where id = {id}").onParams(name, id).executeUpdate()
        prizes.foreach {Prize.update(con, lid, _)}
    }
    Some(true)
  }

  def byId(id: Long) = DB.withConnection(implicit con =>
    SQL(sql + join + " where db.id = {id} order by p.sort").on("id" -> id).as(DrawingBase.withPrizes))

  def byLotteryAndId(lid: Long, id: Long) = DB.withConnection(implicit con =>
    SQL(sql + " where db.lottery_id = {lid} and db.id = {id}" + order).onParams(lid, id).as(DrawingBase.instance.singleOpt))

  def list(lid: Long) = DB.withConnection(implicit con =>
    SQL(sql + join + " where lottery_id = {lid}" + order).onParams(lid).as(DrawingBase.listWithPrizes))

  def delete(id: Long) = {
    DB.withTransaction {
      implicit con => {
        byId(id).map(db => db.prizes.foreach(p => models.lottery.prize.PrizeUpload.deleteByPrize(con, db.lottery.id.get, p.id.get)))
        SQL("delete from dbase where id = {id}").onParams(id).executeUpdate()
        true
      }
    }
  }

  def hasDrawings(id: Long) =
    DB.withConnection(implicit con =>
      SQL("select count(d.id) from drawing d, dbase db where d.drawing_base_id = db.id and db.id = {id}").onParams(id).as(scalar[Long].singleOpt).exists(_ > 0))

  private val withPrizes = (instance ~ Prize.getInstance *).map {
    _.groupBy(_._1).toSeq.headOption.map {
      case (db, prz) => db.copy(prizes = prz.map(_._2))
    }
  }

  private val listWithPrizes = (instance ~ Prize.getInstance *).map {
    _.groupBy(_._1).toSeq.map {
      case (db, prz) => db.copy(prizes = prz.map(_._2))
    }
  }

  // never change this def to a val:
  private def instance = get[Pk[Long]]("dbase.id") ~ get[String]("dbase.name") ~ get[Long]("dbase.lottery_id") ~ get[Long]("dbase.typ") map {
    case id ~ name ~ lid ~ dtid => DrawingBase(id, name, Lottery.byId(lid).get, DrawingType.byId(dtid).get)
  }
}