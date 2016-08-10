package models.lottery.prize

import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._

case class PrizeType(id: Pk[Long], name: String) {}

object PrizeType {

  val CASH = 1
  val MATERIAL = 2

  def list = DB.withConnection(implicit con =>
    SQL("select * from dbase_prizetype order by id asc").as(PrizeType.instance *))

  def byId(id: Long): PrizeType = DB.withConnection(implicit con =>
    SQL("select * from dbase_prizetype where id = {id}").on("id" -> id).as(PrizeType.instance.single))

  private val instance = get[Pk[Long]]("dbase_prizetype.id") ~ get[String]("dbase_prizetype.name") map {
    case id ~ name => PrizeType(id, name)
  }
}