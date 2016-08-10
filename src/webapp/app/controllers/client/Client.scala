package controllers.client

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models.auth.UserConstants._
import models.client.{Client => ClientModel}
import play.api.libs.json._
import org.mindrot.jbcrypt.BCrypt
import play.api.libs.json.Json._
import utils.{Config, PwUtils}
import play.api.i18n.Messages

case class UpdateClient(id: Long,
                        bid: Long,
                        hid: String,
                        salutation: String,
                        firstName: String,
                        lastName: String,
                        password: Option[String])

case class InsertClient(bid: Long, salutation: String, firstName: String, lastName: String, email: String)

object Client extends Controller with ClientAuth {

	val pwMinLength = Config.getString("password.minlength").toInt

	val updateForm = Form[UpdateClient](
		mapping(USER_ID -> longNumber, BRANCH_ID -> longNumber, USER_HASHED_ID -> text, SALUTATION -> nonEmptyText(4, 4), FIRST_NAME -> nonEmptyText(1, 255), LAST_NAME -> nonEmptyText(1, 255), USER_PASSWORD -> optional(text)
		)(UpdateClient.apply)(UpdateClient.unapply)
			verifying("password.minlength", result => result match {
			case (client: UpdateClient) =>
				client.password.map(pw => pwMinLength <= pw.trim().length()).getOrElse(true)
		}))

	val insertForm = Form(
		mapping(
			BRANCH_ID -> longNumber,
			SALUTATION -> nonEmptyText(4, 4),
			FIRST_NAME -> nonEmptyText(1, 255),
			LAST_NAME -> nonEmptyText(1, 255),
			USER_EMAIL -> (email verifying nonEmpty)
		)(InsertClient.apply)(InsertClient.unapply)
			verifying("client.email.exists", result => result match {
			case (client: InsertClient) => ClientModel.nonExistant(client.email, client.bid)
		}))

	def update() = Action {
		implicit request => {
			request.body.asJson.map {
				json =>
					val js = json \ "client"
					val client = js.as[ClientModel]
					Unchanged(client.id.get, client.hid).map {
						ok =>
							ClientModel.byId(client.id.get).map {
								cFromDB =>
									updateForm.bind(json \ "client").fold(
									errorForm => {
										Ok(toJson(JsObject(List("msg" -> JsString("0"), "reason" -> JsString(Messages.apply("password.minlength"))))))
									}, {
										case (c: UpdateClient) =>
											val rm = (json \ "delete").as[String]
											val jstr = ClientModel.update(client, cFromDB.email, rm) match {
												case true => List("msg" -> JsString("1"))
												case _ => List("msg" -> JsString("0"), "reason" -> JsString(Messages.apply("client.update.failure")))
											}
											Ok(toJson(JsObject(jstr)))
									})
							} getOrElse NotFound
					} getOrElse Forbidden
			} getOrElse Forbidden
		}
	}

	def sendPassword() = Action {
		implicit request => {
			request.body.asJson.map {
				json =>
					val email = (json \ "email").as[String]
					val bid = (json \ "blz").as[String].toLong
					val nonExisting = JsObject(List("msg" -> JsString("0"), "reason" -> JsString(Messages.apply("client.email.does.not.exist"))))
					ClientModel.byEmailAndBid(email, bid).map {
						client =>
							val clear = PwUtils.newPassword(pwMinLength)
							val encrypted = BCrypt.hashpw(clear, BCrypt.gensalt())
							ClientModel.updatePassword(client, encrypted, clear) match {
								case 1 => Ok(toJson(JsObject(List("msg" -> JsString("1")))))
								case _ => Ok(toJson(nonExisting))
							}
					} getOrElse Ok(toJson(nonExisting))
			} getOrElse Forbidden
		}
	}

	def add() = Action {
		implicit request => {
			request.body.asJson.map {
				json =>
					val js = json \ "client"
					val client = js.as[ClientModel]
					insertForm.bind(js).fold(
					errorForm => {
						Ok(toJson(JsObject(List("msg" -> JsString("0"), "reason" -> JsString(Messages.apply("client.email.exists"))))))
					}, {
						case (c: InsertClient) =>
							val clear = PwUtils.newPassword(pwMinLength)
							val encrypted = BCrypt.hashpw(clear, BCrypt.gensalt())
							val ret = ClientModel.add(client, encrypted, clear) match {
								case Right(newId) =>
									val savedClient = ClientModel.byId(newId.longValue())
									toJson(JsObject(List("msg" -> JsString("1"), "client" -> toJson(savedClient.get))))
								case _ => toJson(JsObject(List("msg" -> JsString("0"), "reason" -> JsString(Messages.apply("client.add.failed")))))
							}
							Ok(ret)
					})
			} getOrElse Forbidden
		}
	}
}