package controllers.lottery.prize

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import controllers.auth.Auth
import play.api.i18n.Messages
import views.html
import utils.JsonUtils
import play.api.libs.json.{JsObject, Json}
import models.lottery.drawing.{Drawing => DrawingModel, DrawingPrize => DrawingPrizeModel}

object PrizeCount extends Controller with Auth {

	case class CountForm(prizes: String, totalAmount: String)

	val form = Form(mapping("prizes" -> text, "data" -> text)(CountForm.apply)(CountForm.unapply))

	def get(did: Long, hid: String, msg: String = "") = Authorized(Redakteur) {
		implicit request => {
			Unchanged(did, hid).map {
				ok =>
					DrawingModel.byId(did, UserFromSession).map {
						drawing =>
							Ok(html.lottery.drawing.edit_drawing_prize_count(models.lottery.prize.PrizeCount.byDrawing(did), did, html.lottery.nav(), Messages.apply(msg)))
					} getOrElse NotFound
			} getOrElse Forbidden
		}
	}

	def delete(id: Long) = Authorized(Redakteur) {
		implicit request => {
			val result = DrawingPrizeModel.delete(id) match {
				case true => JsonUtils.getStatus(status = true, Messages.apply("pricecount.rm.ok"))
				case _ => JsonUtils.getStatus(status = false, Messages.apply("pricecount.rm.notok"))
			}
			Ok(result).withHeaders(CONTENT_TYPE_JSON)
		}
	}

	def update(did: Long, hid: String) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(did, hid).map {
				ok =>
					form.bindFromRequest().fold(
					errorForm => {
						Ok(JsonUtils.getErrors(errorForm.errors)).withHeaders(CONTENT_TYPE_JSON)
					}, {
						case (prizeform: CountForm) => {

							val pcounts = for (jsobj <- Json.parse(prizeform.prizes).as[List[JsObject]])
							yield models.lottery.prize.PrizeCount(did, (jsobj \ "pid").as[String].toLong, Option((jsobj \ "count").as[String].toLong))

							if (0 < pcounts.size) {
								DrawingModel.byId(did, UserFromSession).map {
									drawing => {
										DrawingModel.updatePrizeCountsAndTotalAmount(did, pcounts)
										Ok(JsonUtils.getStatus(status = true, Messages.apply("input.save.ok"))).withHeaders(CONTENT_TYPE_JSON)
									}
								}.getOrElse(Ok(JsonUtils.getStatus(status = false, "input.save.notok")).withHeaders(CONTENT_TYPE_JSON))
							} else Ok(JsonUtils.getStatus(status = false, "drawing.does.not.exist")).withHeaders(CONTENT_TYPE_JSON)
						}
					})
			} getOrElse Forbidden
		}
	}
}