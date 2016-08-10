package models.lottery.drawing

import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._

case class DrawingType(id: Pk[Long], name: String) {}

object DrawingType {

  private val sql = "select * from drawing_type"

  def list = DB.withConnection(implicit con => SQL(sql).as(DrawingType.instance *))

  def byId(id: Long): Option[DrawingType] = DB.withConnection(implicit con =>
    SQL(sql + " where id = {id}").onParams(id).as(DrawingType.instance.singleOpt))

  private val instance = get[Pk[Long]]("drawing_type.id") ~ get[String]("drawing_type.name") map {
    case id ~ name => DrawingType(id, name)
  }
}