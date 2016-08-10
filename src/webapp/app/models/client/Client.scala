package models.client

import _root_.utils._
import _root_.utils.StringUtils.trim
import utils.mail.Mailer
import org.mindrot.jbcrypt.BCrypt
import play.api.db._
import anorm.SqlParser._
import anorm._
import java.util.Date
import exception.ExceptionCodes
import models.lottery.drawing.Drawing
import play.Logger
import play.api.Play.current
import models.lottery.{Lottery, Branch}
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

case class Client(id: Pk[Long],
                  hid: String,
                  salutation: String,
                  firstName: String,
                  lastName: String,
                  email: Option[String],
                  password: Option[String],
                  query: Option[UserQuery],
                  branch: Branch) {
}

object Client {

	val MAX_LOGIN_ATTEMPTS = 3

	val sqlWithQuery = """ SELECT * FROM branch b, client c
								         LEFT OUTER JOIN client_ticket t ON (t.client_id = c.id)
								         WHERE b.code = c.branch_code """

	val sqlByEmailAndLottery = sqlWithQuery + " AND b.lottery_id = ( SELECT lottery_id FROM branch WHERE code = {code} ) AND email = {email}"

	val sqlClientExists = """ SELECT count(c.id)
                            FROM client c, branch b, lottery l
                            WHERE c.branch_code = b.code
                            AND b.lottery_id = l.id
                            AND b.lottery_id =
                              (
                                SELECT lottery_id FROM branch WHERE code = {code}
                              )
                            AND LOWER(c.email) = {email} """

	val order = " order by c.id, t.start desc"

	def addToWinningNotificationQueue(clients: Seq[Client], drawing: Drawing) = {
		WinningNotifictionCreator.addToWinningNotificationQueue(clients, drawing)
	}

	def listByLotteryAndBranchesWithActiveNotification(lid: Long) = DB.withConnection(implicit con => {
		// only select client if the branch DOES notify its winners => AND b.winning_notification = 1
		SQL(sqlWithQuery + " AND b.lottery_id = {lid} AND b.winning_notification = 1").onParams(lid).as(Client.listWithQuery)
	})

	def byId(id: Long) = DB.withConnection(implicit con => {
		SQL(sqlWithQuery + " and c.id = {id}").onParams(id).as(Client.withQuery)
	})

	def authenticate(email: String, password: String, bid: Long) =
		byActiveEmail(email, bid).map {
			client =>
				try {
					if (!BCrypt.checkpw(password, client.password.getOrElse(""))) {
						Left(ExceptionCodes.WRONG_USERNAME_OR_PASSWORD)
					} else Right(client)
					//  Invalid salt version:
				} catch {
					case ex: Throwable => Left(ExceptionCodes.USER_NOT_FOUND)
				}
		}.getOrElse(Left(ExceptionCodes.USER_NOT_FOUND))

	def byEmailAndBid(email: String, bid: Long): Option[Client] = DB.withConnection(implicit con =>
		SQL(sqlByEmailAndLottery).onParams(bid, email).as(Client.withQuery))

	def nonExistant(email: String, bid: Long): Boolean = DB.withConnection(implicit con =>
		0 == SQL(sqlClientExists).onParams(bid, email.toLowerCase).as(scalar[Long].single))

	def byActiveEmail(email: String, bid: Long) = DB.withConnection(implicit con =>
		SQL(sqlByEmailAndLottery + " AND active = 1" + order).onParams(bid, email).as(Client.withQuery))

	def delete(id: Long) = DB.withConnection {
		implicit con => SQL("delete from client where id = {id}").onParams(id).executeUpdate()
	}

	def updatePassword(client: Client, password: String, clearPassword: String) = DB.withConnection {
		implicit con => {
			val rowsUpdated = SQL("update client set password = {password} where id = {id}").onParams(password, client.id.get).executeUpdate()
			if (1 == rowsUpdated) Future {
				Lottery.byBranchCode(client.branch.code).map {
					Mailer.clientNewPassword(_, client, clearPassword)
				}
			}
			rowsUpdated
		}
	}

	def add(client: Client, password: String, clearPassword: String) = {
		DB.withTransaction {
			implicit con =>
				Lottery.byBranchCode(client.branch.code).map {
					lottery =>
						// do NOT add client if the branch does not notify its winners:
						if (client.branch.hasWinningNotification) {
							try {
								SQL("insert into client(branch_code, salutation, firstname, lastname, email, password, active, created) values ( {branch_code}, {salutation}, {firstname}, {lastname}, {email}, {password}, {active}, {created})")
									.onParams(client.branch.code, trim(client.salutation), trim(client.firstName), trim(client.lastName), trim(client.email.get), password, 1, new Date()).executeUpdate()
								val id = SQL("SELECT last_insert_id()").as(scalar[java.math.BigInteger].single)
								client.query.map(UserQuery.add(con, id.longValue(), _))
								Future {
									val content = getTicketDetails(client)
									Lottery.byBranchCode(client.branch.code).map {
										lottery =>
											Mailer.clientRegister(lottery, client, content, clearPassword)
									}
								}
								Right(id)
							} catch {
								case ex: Throwable => Left(false)
							}
						} else Left(false)
				}.getOrElse(Left(false))
		}
	}

	def update(client: Client, email: Option[String], rm: String) = {
		DB.withTransaction(implicit con => {
			val back = try {
				if ("1" == rm) {
					Client.byId(client.id.get).map {
						c =>
							val email = c.email.getOrElse("undefined")
							val deletedRows = SQL("delete from client where id = {id}").onParams(client.id).executeUpdate()
							if (1 == deletedRows) {
								SQL("insert into client_unregister_history(branch_code, date_deleted) values ({branch_code}, {date_deleted})").onParams(client.branch.code, new Date()).executeUpdate()
								Future {
									Lottery.byBranchCode(c.branch.code).map {
										lottery =>
											Mailer.clientUnregister(lottery, client.copy(email = Option(email)))
									}
								}
							}
					}
				} else {
					SQL("update client set salutation = {salutation}, firstname = {firstname}, lastname = {lastname} where id = {id}").onParams(trim(client.salutation), trim(client.firstName), trim(client.lastName), client.id.get).executeUpdate()
					val pwChanged = if ("" != client.password.getOrElse("")) {
						SQL("update client set password = {pw} where id = {id}").onParams(BCrypt.hashpw(trim(client.password.get), BCrypt.gensalt()), client.id.get).executeUpdate()
						true
					} else false
					client.query.map(UserQuery.update(con, client.id.get, _))
					Future {
						val content = getTicketDetails(client)
						Lottery.byBranchCode(client.branch.code).map {
							lottery =>
								Mailer.clientUpdate(lottery, client.copy(email = email), content, pwChanged)
						}
					}
				}
				true
			} catch {
				case ex: Throwable =>
					Logger.error("Client update: " + ex)
					false
			}
			back
		})
	}

	private def getTicketDetails(client: Client) = {
		val body =
			client.query.fold("")(_.ranges.flatten.foldLeft(List[String]()) {
				(acc, range) => {
					if (range.fromStr != range.toStr)
						(new StringBuilder().append("Losnummernbereich: ") append range.fromStr append "-" append range.toStr).toString :: acc
					else
						(new StringBuilder().append("Losnummer: ") append range.fromStr).toString :: acc
				}
			}.mkString("\n"))
		body + (if ("" == body) "Mit freundlichen Grüßen" else "\n\n" + "Mit freundlichen Grüßen")
	}

	private val withQuery = (Client.instance ~ QueryTicketRange.instance *).map {
		_.groupBy(_._1).toSeq.headOption.map {
			case (c, qtr) => c.copy(query = Option(UserQuery(qtr.map(tpl => tpl._2))))
		}
	}

	private val listWithQuery = (Client.instance ~ QueryTicketRange.instance *).map {
		_.groupBy(_._1).toSeq.map {
			case (c, qtr) => c.copy(query = Option(UserQuery(qtr.map(tpl => tpl._2))))
		}
	}

	private[client] def instance = get[Pk[Long]]("client.id") ~ get[Long]("branch.code") ~ get[String]("client.salutation") ~
		get[String]("client.firstname") ~ get[String]("client.lastname") ~ get[String]("client.email") ~ get[String]("client.password") map {
		case id ~ bcode ~ sal ~ fname ~ lname ~ email ~ pw =>
			Client(id, FieldEncrypt.sign(id.get), sal, fname, lname, Option(email), Option(pw), None, Branch.byCode(bcode))
	}
}
