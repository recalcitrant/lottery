package controllers.lottery

import play.api.mvc._
import views.html
import controllers.auth.{UserNav, Auth}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json._
import play.api.libs.json._
import models.lottery.{Lottery => LotteryModel}

object Misc extends Controller with Auth with UserNav {

  private val form = Form(mapping("data" -> text)(JsonForm.apply)(JsonForm.unapply))

  case class JsonForm(data: String)

  def getDrawingsVisible(lid: Long) = Authorized(Redakteur) {
    implicit request =>
      val res = LotteryModel.getDrawingsVisible(lid).getOrElse(6)
      Ok(toJson(JsObject(List("res" -> JsString(res.toString)))))
  }

  def setDrawingsVisible(lid: Long) = Authorized(Redakteur) {
    implicit request =>
      form.bindFromRequest().fold(
      errorForm => Forbidden, {
        case (form: JsonForm) =>
          val res = LotteryModel.updateDrawingsVisible(Json.parse(form.data).as[String].toInt, lid)
          Ok(toJson(res.map(b => JsObject(List("msg" -> JsString("ok")))).getOrElse(JsObject(List("msg" -> JsString("notok"))))))
      })
  }

  def list = {
    Authorized(Redakteur) {
      implicit request => {
        Ok(html.lottery.misc(models.lottery.Template.list, LotteryId, html.lottery.nav()))
      }
    }
  }
}