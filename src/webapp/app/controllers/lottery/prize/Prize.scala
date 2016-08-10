package controllers.lottery.prize

import play.api.mvc._
import play.api.data.Forms._
import utils.JsonUtils
import play.api.libs.json.Json._
import play.api.data._
import controllers.auth.Auth
import play.api.i18n.Messages
import anorm.NotAssigned
import models.lottery.prize.{Prize => PrizeModel, PrizeType}
 
import play.api.libs.json.Json

object Prize extends Controller with Auth {

	case class PrizeForm(prize: String)

	case class DescForm(desc: String)

	case class TitleForm(title: String)

	val form = Form(mapping("prize" -> text)(PrizeForm.apply)(PrizeForm.unapply))
	val descform = Form(mapping("description" -> text)(DescForm.apply)(DescForm.unapply))
	val titleform = Form(mapping("title" -> text)(TitleForm.apply)(TitleForm.unapply))

	def getDescription(id: Long) = Authorized(Redakteur) {
		implicit request =>
			Ok(toJson(PrizeModel.getDescription(id).getOrElse("")))
	}

	def getTitle(id: Long) = Authorized(Redakteur) {
		implicit request =>
			Ok(toJson(PrizeModel.getTitle(id).getOrElse("")))
	}

	def updateTitle(id: Long) = Authorized(Redakteur) {
		implicit request => {
			titleform.bindFromRequest().fold(
			errorForm => {
				Ok(JsonUtils.getErrors(errorForm.errors)).withHeaders(CONTENT_TYPE_JSON)
			}, {
				case (titleform: TitleForm) =>
					val result = PrizeModel.updateTitle(id, titleform.title) match {
						case Some(i) => JsonUtils.getStatus(status = true, Messages.apply("input.save.ok"))
						case None => JsonUtils.getStatus(status = false, Messages.apply("input.save.notok"))
					}
					Ok(result).withHeaders(CONTENT_TYPE_JSON)
			})
		}
	}

	def updateDescription(id: Long) = Authorized(Redakteur) {
		implicit request => {
			descform.bindFromRequest().fold(
			errorForm => {
				Ok(JsonUtils.getErrors(errorForm.errors)).withHeaders(CONTENT_TYPE_JSON)
			}, {
				case (descform: DescForm) =>
					val result = PrizeModel.updateDescription(id, descform.desc) match {
						case Some(i) => JsonUtils.getStatus(status = true, Messages.apply("prize.description.update.ok"))
						case None => JsonUtils.getStatus(status = false, Messages.apply("prize.description.update.notok"))
					}
					Ok(result).withHeaders(CONTENT_TYPE_JSON)
			})
		}
	}

	def add(dbid: Long) = Authorized(Redakteur_OR_Freigeber) {
		implicit request => {
			form.bindFromRequest().fold(
			errorForm => {
				Ok(JsonUtils.getErrors(errorForm.errors)).withHeaders(CONTENT_TYPE_JSON)
			}, {
				case (prizeform: PrizeForm) =>
					val jsobj = Json.parse(prizeform.prize)
					val prize = PrizeModel(
						NotAssigned,
						(jsobj \ "value").as[String],
						None,
						None,
						PrizeType.byId((jsobj \ "typ").as[String].toLong),
						(jsobj \ "sort").as[Long])
					val result = PrizeModel.addViaTransaction(dbid, prize).map {
						newid => JsonUtils.map(Map("status" -> "ok", "newid" -> newid.longValue().toString, "msg" -> Messages.apply("prize.add.ok")))
					}.getOrElse(JsonUtils.getStatus(status = false, Messages.apply("prize.add.notok")))
					Ok(result).withHeaders(CONTENT_TYPE_JSON)
			})
		}
	}

	def delete(id: Long, did: Long) = Authorized(Redakteur) {
		implicit request => {
			val result = PrizeModel.delete(id, did) match {
				case true => JsonUtils.getStatus(status = true, Messages.apply("prize.rm.ok"))
				case _ => JsonUtils.getStatus(status = false, Messages.apply("prize.rm.notok"))
			}
			Ok(result).withHeaders(CONTENT_TYPE_JSON)
		}
	}
}