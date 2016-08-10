package net.yogasurftech.sfp.lottery.clientimport

import java.sql.Date
import scala.slick.driver.MySQLDriver.simple._
import Database.threadLocalSession
import scala.io.Source
import scala.collection.immutable.HashSet
import org.mindrot.jbcrypt.BCrypt

object ClientImporter {

	def importClients() {

		object Client extends Table[(Option[Long], Long, String, String, String, String, String, Date, Int)]("client") {
			def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
			def branchCode = column[Long]("branch_code")
			def email = column[String]("email")
			def password = column[String]("password")
			def salutation = column[String]("salutation")
			def firstName = column[String]("firstname")
			def lastName = column[String]("lastname")
			def created = column[Date]("created")
			def active = column[Int]("active")
			def * = id.? ~ branchCode ~ email ~ password ~ salutation ~ firstName ~ lastName ~ created ~ active
		}

		object ClientTicket extends Table[(Option[Long], Long, String, String)]("client_ticket") {
			def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
			def clientId = column[Long]("client_id")
			def start = column[String]("start")
			def end = column[String]("end")
			def * = id.? ~ clientId ~ start ~ end
		}

		case class Client(id: Option[Long], branchCode: Long, email: String, salutation: String, firstName: String, lastName: String, tickets: Set[Ticket])
		case class Ticket(number: String)

		var lastmail = ""
		val branch_code = 35050000l
		var tickets = new HashSet[Ticket]
		var clients = new HashSet[Client]
		var client = Client(None, -1l, "", "", "", "", Set[Ticket]())

		// java -Dimportfile=/path/to/file -jar clientimport-assembly-0.0.1.jar
		val file = System.getProperty("importfile")
		Source.fromFile(file, "ISO-8859-1").getLines().foreach {
			line => {
				val arrLine = line.split(";")
				if (6 != arrLine.size) throw new RuntimeException
				else {
					val actualmail = arrLine(0).trim
					val salutation = arrLine(5).trim
					val firstName = arrLine(1).trim
					val lastName = arrLine(2).trim
					val ticketAmountstr = arrLine(3).trim
					val ticketAmount = if (ticketAmountstr == "") 0 else ticketAmountstr.toInt
					val ticket: String =
						if (arrLine(4).equals("")) {
							if (ticketAmount > 0) throw new RuntimeException
							else "-1"
						} else {
							if (ticketAmount == 0) throw new RuntimeException
							else arrLine(4).trim
						}
					if (actualmail != lastmail) {
						clients += Client(None, client.branchCode, client.email, client.salutation, client.firstName, client.lastName, client.tickets)
						tickets = new HashSet[Ticket]
					}
					if (ticket != "-1") tickets += Ticket(ticket)
					lastmail = actualmail
					client = Client(None, branch_code, actualmail, salutation, firstName, lastName, tickets)
				}
			}
		}

		Database.forURL("jdbc:mysql://localhost:3306/lottery", driver = "com.mysql.jdbc.Driver", user = System.getProperty("lotteryuser"), password = System.getProperty("lotterypassword")) withSession {
			clients.foreach {
				c => {
					val cid = Client.returning(Client.id) insert(None, branch_code, c.email, BCrypt.hashpw("a7R48Kl2EqWs&(/!dztMb,-:;?" + System.currentTimeMillis() + Math.random(), BCrypt.gensalt()), c.salutation, c.firstName, c.lastName, new Date(new java.util.Date().getTime), 1)
					c.tickets foreach (t => ClientTicket.insert(None, cid, t.number, t.number))
				}
			}
		}
	}
}