package controllers.lottery.dbase

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views.html
import controllers.auth.Auth
import play.api.i18n.Messages
import models.lottery.prize.PrizeType
import utils.{FieldEncrypt, JsonUtils}
import anorm.{Id, NotAssigned}
import models.lottery.drawing.DrawingType
import play.api.libs.json._
import models.lottery.dbase.DrawingBase
import models.lottery.prize.{Prize => PrizeModel}

object DBase extends Controller with Auth {

	case class BaseForm(name: String, prizes: String)

	val form = Form(mapping("name" -> text, "prizes" -> text)(BaseForm.apply)(BaseForm.unapply))

	def prize(id: Long, lid: Long) = Authorized(Redakteur_OR_Freigeber) {
		implicit request => Ok(html.lottery.dbase.show_mat_prize(PrizeModel.byId(id), lid))
	}

	def add(lid: Long, hlid: String, dtype: Long) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(lid, hlid).map {
				ok =>
					form.bindFromRequest().fold(
					errorForm => {
						Ok(JsonUtils.getErrors(errorForm.errors)).withHeaders(CONTENT_TYPE_JSON)
					}, {
						case (dbase: BaseForm) =>
							val prizes = for (jsobj <- Json.parse(dbase.prizes).as[Seq[JsObject]])
							yield PrizeModel(
									NotAssigned,
									(jsobj \ "value").as[String],
									None,
									None,
									PrizeType.byId((jsobj \ "typ").as[String].toLong),
									(jsobj \ "sort").as[Long])
							// manual validation since not yet possible with Play2.0 :
							if (0 < prizes.size && dbase.name.trim() != "") {
								val result = DrawingBase.add(lid, dbase.name, dtype, prizes).map {
									id => JsonUtils.map(Map("status" -> "ok", "dbid" -> id.longValue().toString, "hid" -> FieldEncrypt.sign(id.longValue()), "msg" -> "drawingbase.save.ok"))
								}.getOrElse(JsonUtils.getStatus(status = false, "drawingbase.save.notok"))
								Ok(result).withHeaders(CONTENT_TYPE_JSON)
							} else Ok(JsonUtils.getStatus(status = false, "drawingbase.save.notok")).withHeaders(CONTENT_TYPE_JSON)
					})
			} getOrElse Forbidden
		}
	}

	def update(id: Long, hid: String) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(id, hid).map {
				ok =>
					form.bindFromRequest().fold(
					errorForm => {
						Ok(JsonUtils.getErrors(errorForm.errors)).withHeaders(CONTENT_TYPE_JSON)
					}, {
						case (dbase: BaseForm) =>
							val prizes = for (jsobj <- Json.parse(dbase.prizes).as[Seq[JsObject]])
							yield PrizeModel(
									Id((jsobj \ "id").as[String].toLong),
									(jsobj \ "value").as[String],
									None,
									None,
									PrizeType.byId((jsobj \ "typ").as[String].toLong),
									(jsobj \ "sort").as[Long])
							// manual validation since not yet possible with Play2.0 :
							if (0 < prizes.size && dbase.name.trim() != "") {
								if (DrawingBase.hasDrawings(id)) Ok(JsonUtils.getStatus(status = false, Messages.apply("drawing.exists.for.this.base"))).withHeaders(CONTENT_TYPE_JSON)
								else {
									val result = DrawingBase.update(id, LotteryId, dbase.name, prizes).map {
										success => JsonUtils.map(Map("status" -> "ok", "msg" -> Messages.apply("drawingbase.edit.ok")))
									}.getOrElse(JsonUtils.getStatus(status = false, Messages.apply("drawingbase.edit.notok")))
									Ok(result).withHeaders(CONTENT_TYPE_JSON)
								}
							} else Ok(JsonUtils.getStatus(status = false, Messages.apply("drawingbase.edit.notok"))).withHeaders(CONTENT_TYPE_JSON)
					})
			} getOrElse Forbidden
		}
	}

	def delete(id: Long, hid: String) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(id, hid).map {
				ok =>
					DrawingBase.byLotteryAndId(LotteryId, id).map {
						baseBelongingToLottery =>
							if (DrawingBase.hasDrawings(id))
								Ok(JsonUtils.getStatus(status = false, "drawing.exists.for.this.base")).withHeaders(CONTENT_TYPE_JSON)
							else {
								val result = DrawingBase.delete(id) match {
									case true => JsonUtils.getStatus(status = true, "drawing.base.rm.ok")
									case _ => JsonUtils.getStatus(status = false, "drawing.base.rm.notok")
								}
								Ok(result).withHeaders(CONTENT_TYPE_JSON)
							}
					}.getOrElse(Ok(JsonUtils.getStatus(status = false, "drawing.base.does.not.exist.in.lottery")).withHeaders(CONTENT_TYPE_JSON))
			} getOrElse Forbidden
		}
	}

	def shownew(lid: Long, hlid: String, dtype: Long) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(lid, hlid).map {
				ok =>
					Ok(html.lottery.dbase.new_drawing_base(PrizeType.list, lid, DrawingType.byId(dtype), html.lottery.nav(), Messages.apply("prizetype.info")))
			}.getOrElse(TamperedWithParam)
		}
	}

	def list(lid: Long, hlid: String, msg: String = "") = Authorized(Redakteur_OR_Freigeber) {
		implicit request => {
			Unchanged(lid, hlid).map {
				ok =>
					val dbaseList = DrawingBase.list(lid).groupBy(_.drawingType.id.get).toSeq.map {
						tpl => (tpl._2.head.drawingType.name, tpl._2)
					}.sortWith((a, b) => a._2.head.name > b._2.head.name)
					val typeList = models.lottery.drawing.DrawingType.list
					Ok(html.lottery.dbase.list_drawing_bases(dbaseList, typeList, lid, html.lottery.nav(), msg))
			} getOrElse Forbidden
		}
	}

	def get(id: Long) = Authorized(Redakteur_OR_Freigeber) {
		implicit request =>
			Ok(html.lottery.dbase.show_drawing_base(DrawingBase.byId(id).get, LotteryId))
	}

	def edit(id: Long, hid: String, msg: String = "") = Authorized(Redakteur_OR_Freigeber) {
		implicit request =>
			Unchanged(id, hid).map {
				ok =>
					Ok(html.lottery.dbase.edit_drawing_base(models.lottery.dbase.DrawingBase.byId(id).get, PrizeType.list, html.lottery.nav(), msg))
			} getOrElse Forbidden
	}
}