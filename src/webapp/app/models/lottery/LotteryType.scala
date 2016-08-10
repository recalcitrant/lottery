package models.lottery

import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._

case class LotteryType(id: Pk[Long], name: String)

object LotteryType {

  val CLASSIC = 1
  val EXTENDED = 2

  def byId(id: Long): LotteryType = DB.withConnection(implicit con =>
    SQL("select * from lottery_type where id = {id}").onParams(id).as(LotteryType.instance.single))

  protected[lottery] val instance = get[Pk[Long]]("lottery_type.id") ~ get[String]("lottery_type.name") map {case id ~ name => LotteryType(id, name)}

}