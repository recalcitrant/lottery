package models.client

import play.api.libs.json.Json._
import play.api.libs.json._
import anorm.{Id, NotAssigned}
import utils.NumberUtils._
import utils.StringUtils._
import utils.{Config, FieldEncrypt}
import models.lottery.Branch

object ClientJsonFormat {

	private val maxRange = Config.getLong("tickets.max.range")
	private val stripLeadingZeros = "\\b0*([1-9][0-9]*|0)\\b".r

	implicit object RangeFormat extends Format[Option[QueryTicketRange]] {

		def reads(json: JsValue): JsResult[Option[QueryTicketRange]] = {
			val from = (json \ "from").as[String].trim
			val to = (json \ "to").as[String].trim
			val nfrom = str2Long(stripLeadingZeros replaceAllIn(from, "$1"))
			val nto = str2Long(stripLeadingZeros replaceAllIn(to, "$1"))
			// from & to might be in the wrong order so swap them if needed:
			if (numbersOnly(from) && numbersOnly(to)) {
				if (nfrom <= nto) {
					// check allowed maximum range
					if (nto - nfrom <= maxRange) JsSuccess(Option(QueryTicketRange(from, to, nfrom, nto)))
					else JsSuccess(None)
				} else {
					// check allowed maximum range
					if (nfrom - nto <= maxRange) JsSuccess(Option(QueryTicketRange(to, from, nto, nfrom)))
					else JsSuccess(None)
				}
			} else JsSuccess(None)
		}

		def writes(ranges: Option[QueryTicketRange]): JsValue =
			JsObject(List("from" -> JsString(ranges.map(_.fromStr).getOrElse("")), "to" -> JsString(ranges.map(_.toStr).getOrElse(""))))
	}

	implicit object UserQueryFormat extends Format[UserQuery] {
		def reads(json: JsValue): JsResult[UserQuery] = JsSuccess(UserQuery(json.asOpt[List[Option[QueryTicketRange]]].getOrElse(List[Option[QueryTicketRange]]())))

		def writes(query: UserQuery): JsValue = toJson(query.ranges)
	}

	implicit object ClientFormat extends Format[Client] {

		def reads(json: JsValue): JsResult[Client] = {
			val bcode = (json \ "blz").as[String]
			val branch = Branch.byCode(bcode.toLong)
			val id = (json \ "id").as[Option[String]]
			val hid = (json \ "hid").as[Option[String]]
			val salutation = (json \ "salutation").as[String]
			val firstname = (json \ "firstname").as[String]
			val lastname = (json \ "lastname").as[String]
			val email: Option[String] = (json \ "email").as[Option[String]]
			val password: Option[String] = (json \ "password").as[Option[String]]
			val query = (json \ "tickets").as[Option[UserQuery]]
			JsSuccess(models.client.Client(id.map(id => Id(id.toLong)).getOrElse(NotAssigned), hid.getOrElse("empty"), salutation, firstname, lastname, email, password, query, branch))
		}

		def writes(client: Client): JsValue =
			JsObject(List(
				"id" -> JsString(client.id.map(_.toString).getOrElse("-1")),
				"hid" -> JsString(client.id.map(id => FieldEncrypt.sign(id)).getOrElse(("-1"))),
				"blz" -> JsString(client.branch.code.toString),
				"salutation" -> JsString(client.salutation.toString),
				"firstname" -> JsString(client.firstName),
				"lastname" -> JsString(client.lastName),
				"email" -> JsString(client.email.getOrElse("undefined")), /*
        "password" -> JsString(client.password)*/
				"tickets" -> client.query.map(query => foldRanges(query.ranges)).get))

		def foldRanges(ranges: Seq[Option[QueryTicketRange]]) = {
			ranges.foldLeft(JsArray()) {
				(acc, ticket) => acc :+ toJson(ticket)
			}
		}
	}

}
