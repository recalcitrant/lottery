package models.lottery.statistics

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.util.Date
import models.lottery.statistics.StatisticsSQL._
import utils.db.BlockingJDBC

case class Stats(year: Int, month: Int, count: Long)

object StatisticsType {
	val TYPE_OVERVIEW = 0
	val TYPE_QUERY = 1
}

object Statistics extends BlockingJDBC {

	def getByBranchCodeDrawings(bcode: Long, typ: Int) = DB.withConnection(implicit con => SQL(hitsPerBranchDrawing).onParams(bcode, typ).as(hitsInstance *))

	def getByLidDrawings(lid: Long, typ: Int) = DB.withConnection(implicit con => SQL(hits).onParams(lid, typ).as(hitsInstance *))

	def getAddedUsersByBranch(bcode: Long) = DB.withConnection(implicit con => SQL(selectUsers + hitsPerBranchUsers + tailUsers).onParams(bcode).as(hitsInstance *))

	def getUnregisteredUsersByLottery(lid: Long) = DB.withConnection(implicit con => SQL(selectUnregisteredUsers + whereUnregisteredUsers + tailUnregisteredUsers).onParams(lid).as(hitsInstance *))

	def getUnregisteredUsersByBranch(bcode: Long) = DB.withConnection(implicit con => SQL(selectUnregisteredUsers + unregisteredPerBranchUsers + tailUnregisteredUsers).onParams(bcode).as(hitsInstance *))

	def getAddedUsersByLottery(lid: Long) = DB.withConnection(implicit con => SQL(selectUsers + lot + tailUsers).onParams(lid).as(hitsInstance *))

	def add(bcode: String, stype: Int) = {
		sqlWithFuture { implicit con => SQL(insertSql).onParams(stype, bcode, new Date()).executeUpdate() }
	}

	private def hitsInstance = get[Int]("year") ~ get[Int]("month") ~ get[Long]("count") map {
		case year ~ month ~ count => Stats(year, month, count)
	}
}
