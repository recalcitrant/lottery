package controllers.lottery.drawing

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views.html
import controllers.auth.Auth
import models.workflow.Workflow
import anorm.NotAssigned

import utils.{FieldEncrypt, DateUtils, JsonUtils}
import models.lottery.drawing.{Drawing => DrawingModel, DrawingPrize => DrawingPrizeModel, TicketDigit}
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}

object Drawing extends Controller with Auth {

	case class BaseForm(data: String, prizes: String)

	val form = Form(mapping("data" -> text, "prizes" -> text)(BaseForm.apply)(BaseForm.unapply))

	def shownew(dbid: Long, hdbid: String) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(dbid, hdbid).map {
				ok =>
					val dbase = models.lottery.dbase.DrawingBase.byId(dbid).get
					Ok(html.lottery.drawing.new_drawing(dbase, html.lottery.nav()))
			}.getOrElse(Ok(html.changed_param()))
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
						case (dbase: BaseForm) =>
							val jsdates = Json.parse(dbase.data)
							val date = (jsdates \ "date").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2Date(s)) else None)
							val datenext = (jsdates \ "datenext").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2Date(s)) else None)
							val datepublishnext = (jsdates \ "datepublishnext").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2Date(s)) else None)
							val date_winning_notification = (jsdates \ "date_winning_notification").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2DateHourMinutes(s)) else None)
							val drawing_dates = (date, datenext, datepublishnext, date_winning_notification)
							val prizes = for (jsobj <- Json.parse(dbase.prizes).as[List[JsObject]]) yield Option(((jsobj \ "dpid").as[String].toLong, (jsobj \ "digits").as[String], (jsobj \ "winning_notification").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2DateHourMinutes(s)) else None)))
							DrawingModel.byId(did, UserFromSession).map {
								drawing =>
									val flowok = drawing.workflow.map(flow => Workflow.STATE_OFFEN == flow.state.id || Workflow.STATE_ZURUECKGEWIESEN == flow.state.id).getOrElse(true)
									if (flowok) {
										val result = DrawingModel.update(did, prizes, drawing_dates._1, drawing_dates._2, drawing_dates._3, drawing_dates._4).map {
											success => JsonUtils.map(Map("status" -> "ok", "msg" -> "drawing.save.ok"))
										}.getOrElse(JsonUtils.getStatus(status = false, "drawing.save.notok"))
										Ok(result).withHeaders(CONTENT_TYPE_JSON)
									} else Ok(JsonUtils.getStatus(status = false, "drawing.workflow.state.forbids.edit")).withHeaders(CONTENT_TYPE_JSON)
							}.getOrElse(Ok(JsonUtils.getStatus(status = false, "drawing.does.not.exist")).withHeaders(CONTENT_TYPE_JSON))
					})
			} getOrElse Forbidden
		}
	}

	def add(dbid: Long) = Authorized(Redakteur) {
		implicit request => {
			val dbase = models.lottery.dbase.DrawingBase.byId(dbid).get
			form.bindFromRequest().fold(
			errorForm => {
				Ok(JsonUtils.getErrors(errorForm.errors)).withHeaders(CONTENT_TYPE_JSON)
			}, {
				case dform: BaseForm =>
					val jsdates = Json.parse(dform.data)
					val date = (jsdates \ "date").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2Date(s)) else None)
					val datenext = (jsdates \ "datenext").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2Date(s)) else None)
					val datepublishnext = (jsdates \ "datepublishnext").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2Date(s)) else None)
					val date_winning_notification = (jsdates \ "date_winning_notification").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2DateHourMinutes(s)) else None)
					val drawing_dates = (date, datenext, datepublishnext, date_winning_notification)
					val prizes =
						for (jsobj <- Json.parse(dform.prizes).as[List[JsObject]])
						yield {
							val fd = (jsobj \ "digits").as[Option[String]].flatMap {
								digs => {
									if (digs != "") {
										val dateWinningNotification = (jsobj \ "winning_notification").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2DateHourMinutes(s)) else None)
										Option(TicketDigit(NotAssigned, digs, dateWinningNotification))
									} else None
								}
							}
							(models.lottery.prize.Prize.byId((jsobj \ "id").as[String].toLong), fd)
						}

					val dprz = for ((price, finalDigits) <- prizes) yield DrawingPrizeModel(NotAssigned, price, finalDigits)
					val drawing = DrawingModel(NotAssigned, dbase, dprz, None, None, None, None, drawing_dates._1, drawing_dates._2, drawing_dates._3, drawing_dates._4)
					val result = DrawingModel.add(drawing).map {
						id => JsonUtils.map(Map("status" -> "ok", "did" -> id.longValue().toString, "hid" -> FieldEncrypt.sign(id.longValue()), "msg" -> "drawing.save.ok.input.prize.count"))
					}.getOrElse(JsonUtils.getStatus(status = false, Messages.apply("drawing.save.notok")))
					Ok(result).withHeaders(CONTENT_TYPE_JSON)
			})
		}
	}

	def edit(id: Long, hid: String, msg: String = "") = Authorized(Redakteur) {
		implicit request =>
			Unchanged(id, hid).map {
				ok =>
					Ok(html.lottery.drawing.edit_drawing(DrawingModel.byId(id, UserFromSession).get, html.lottery.nav(), msg))
			} getOrElse Forbidden
	}

	def delete(id: Long, hid: String) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(id, hid).map {
				ok =>
					DrawingModel.byId(id, UserFromSession).map {
						drawing =>
							drawing.workflow.map {
								flow =>
									if (Workflow.STATE_OFFEN == flow.state.id || Workflow.STATE_ZURUECKGEWIESEN == flow.state.id) Ok(deleteDrawing(LotteryId, id)).withHeaders(CONTENT_TYPE_JSON)
									else Ok(JsonUtils.getStatus(status = false, "drawing.workflow.state.forbids.deletion")).withHeaders(CONTENT_TYPE_JSON)
							}.getOrElse(Ok(deleteDrawing(LotteryId, id)).withHeaders(CONTENT_TYPE_JSON))
					}.getOrElse(Ok(JsonUtils.getStatus(status = false, "drawing.does.not.exist")).withHeaders(CONTENT_TYPE_JSON))
			} getOrElse Forbidden
		}
	}

	def list(msg: String = "") = Authorized(Redakteur_OR_Freigeber) {
		implicit request => {
			// 108 == LotteryId => isRSGV
			Ok(html.lottery.drawing.list_drawings(DrawingModel.list(UserFromSession), 108 == LotteryId, LotteryType, html.lottery.nav(), Messages.apply(msg)))
		}
	}

	def get(id: Long) = Authorized(Redakteur_OR_Freigeber) {
		implicit request =>
			DrawingModel.byId(id, UserFromSession).map {
				drawing =>
					Ok(html.lottery.drawing.show_drawing(drawing))
			} getOrElse NotFound
	}

	private def deleteDrawing(lid: Long, id: Long) = {
		DrawingModel.delete(lid, id) match {
			case Some(i) => JsonUtils.getStatus(status = true, "drawing.rm.ok")
			case None => JsonUtils.getStatus(status = false, "drawing.does.not.exist")
		}
	}
}
