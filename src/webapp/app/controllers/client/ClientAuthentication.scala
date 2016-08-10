package controllers.client

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.libs.json.Json._
import play.api.libs.json.{JsString, JsObject}
import play.api.i18n.Messages

object ClientAuthentication extends Controller with ClientAuth {

  val loginForm = Form(tuple("email" -> (email verifying nonEmpty), "password" -> nonEmptyText(8, 255), "blz" -> longNumber))

  def logout = Action {implicit request => Ok}

  def authenticate = Action {
    implicit request => {
      request.body.asJson.map {
        json =>
          loginForm.bind(json).fold(
          errors => {
            Ok(toJson(JsObject(List("msg" -> JsString("0"), "reason" -> JsString(Messages.apply("wrong.username.or.password"))))))
          }, {
            case (mail, pw, bcode) =>
              models.client.Client.authenticate(mail.trim, pw.trim, bcode) match {
                case ex@Left(c: Int) => Ok(toJson(JsObject(List("msg" -> JsString("0"), "reason" -> JsString(Messages.apply("wrong.username.or.password"))))))
                case Right(client) =>
                  Ok(toJson(JsObject(List("msg" -> JsString("1"), "client" -> toJson(client)))))
              }
          })
      } getOrElse Forbidden
    }
  }
}