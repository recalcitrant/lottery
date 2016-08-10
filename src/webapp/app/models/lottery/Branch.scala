package models.lottery

import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._

case class Branch(id: Pk[Long], code: Long, name: String, url: String, loginUrl: String, hasWinningNotification: Boolean)

object Branch {

	def allByLottery(lid:Long) = DB.withConnection(implicit con =>
			SQL("select * from branch where lottery_id = {lid}").onParams(lid).as(instance *))

	def byCode(bcode: Long) = DB.withConnection(implicit con =>
		SQL("select * from branch where code = {bcode}").onParams(bcode).as(instance.single))

	protected[lottery] val instance = get[Pk[Long]]("branch.id") ~ get[Long]("branch.code") ~
		get[String]("branch.name") ~ get[String]("branch.url") ~ get[String]("branch.url_login") ~ get[Int]("branch.winning_notification") map {
		case id ~ code ~ name ~ url ~ loginUrl ~ haswinningNotif => Branch(id, code, name, url, loginUrl, 1 == haswinningNotif)
	}
}
