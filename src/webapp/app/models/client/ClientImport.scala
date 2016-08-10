package models.client

import scala.io.Source
import java.io.File
import java.util.Date
import play.api.db._
import anorm._
import anorm.SqlParser._

object ClientImport {

	case class Ticket(num: String)

	case class Candidate(id: Pk[Long], sal: String, email: String, first: String, last: String, tickets: Seq[Ticket]) {

		def add = {
			import play.api.Play.current
			DB.withTransaction {
				implicit con =>
					SQL("insert into client(branch_code, salutation, firstname, lastname, email, password, active, created) values ( {branch_code}, {salutation}, {firstname}, {lastname}, {email}, {password}, {active}, {created})")
						.onParams(35450000, sal, first, last, email, "16zjc21aa32a17917f8f88163ea05e46", 1, new Date()).executeUpdate()
					SQL("SELECT last_insert_id()").as(scalar[java.math.BigInteger].singleOpt).map(cid => {
						for (ticket <- tickets) SQL("insert into client_ticket (client_id, start, end) values ({cid}, {start}, {end})").onParams(cid.longValue(), ticket.num, ticket.num).executeUpdate()
					})
			}
		}
	}

	def parse(file: File) {
		val stripLeadingZeros = "\\b0*([1-9][0-9]*|0)\\b".r
		try {
			var lastCandidate = Candidate(NotAssigned, "Frau", "", "", "", Seq[Ticket]())
			Source.fromFile(file).getLines().foreach {
				line => {
					val split = line.trim.split("\t")
					if (6 == split.length) {
						val ticket = Ticket(stripLeadingZeros replaceAllIn(split(5), "$1"))
						val candidate = Candidate(NotAssigned, split(0), split(1), split(2), split(3), Seq[Ticket]() :+ ticket)
						if (candidate.email != lastCandidate.email) {
							if (0 < lastCandidate.tickets.size) {
								lastCandidate.add
							}
							lastCandidate = candidate.copy()
						} else {
							lastCandidate = candidate.copy(tickets = lastCandidate.tickets :+ ticket)
						}
					}
				}
			}
		} catch {
			case ex: Throwable => println(ex)
		}
	}
}