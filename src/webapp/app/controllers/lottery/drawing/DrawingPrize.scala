package controllers.lottery.drawing

import play.api.mvc._
import play.api.data.Forms._
import utils.{DateUtils, JsonUtils}
import play.api.data._
import controllers.auth.Auth
import play.api.i18n.Messages
import anorm.NotAssigned
import play.api.libs.json.Json

object DrawingPrize extends Controller with Auth {

  case class PrizeForm(prize: String)

  val form = Form(mapping("prize" -> text)(PrizeForm.apply)(PrizeForm.unapply))

  def delete(id: Long) = Authorized(Redakteur) {
    implicit request => {
      val result = models.lottery.drawing.DrawingPrize.delete(id) match {
        case true => JsonUtils.getStatus(status = true, Messages.apply("drawingprize.rm.ok"))
        case _ => JsonUtils.getStatus(status = false, Messages.apply("drawingprize.rm.notok"))
      }
      Ok(result).withHeaders(CONTENT_TYPE_JSON)
    }
  }

  def add(did: Long) = Authorized(Redakteur) {
    implicit request => {
      form.bindFromRequest().fold(
      errorForm => {
        Ok(JsonUtils.getErrors(errorForm.errors)).withHeaders(CONTENT_TYPE_JSON)
      }, {
        case (prizeform: PrizeForm) =>
          val jsprize = Json.parse(prizeform.prize)
          val pid = (jsprize \ "pid").as[String].toLong
          val fdo = (jsprize \ "digits").as[Option[String]].flatMap(d =>
            if (d != "") {
              val dateWinningNotification = (jsprize \ "date_winning_notification").as[Option[String]].flatMap(s => if (s != "") Some(DateUtils.str2DateHourMinutes(s)) else None)
              Option(models.lottery.drawing.TicketDigit(NotAssigned, d, dateWinningNotification))
            }
            else None
          )
          val dprize = models.lottery.drawing.DrawingPrize(NotAssigned, models.lottery.prize.Prize.byId(pid), fdo)
          models.lottery.drawing.DrawingPrize.add(dprize, did).map {
            newpid =>
              Ok(JsonUtils.map(Map("status" -> "ok", "dpid" -> newpid.toString, "msg" -> Messages.apply("digits_and_count.save.ok")))).withHeaders(CONTENT_TYPE_JSON)
          } getOrElse NotFound
      })
    }
  }
}