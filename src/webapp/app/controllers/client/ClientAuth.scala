package controllers.client

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.Crypto
import play.api.Play
import play.api.Play.current
import play.api.libs.json.Json._
import play.api.libs.json._

trait ClientAuth {

  def Unchanged(field: Long, hash: String) = {
    hash == Crypto.sign(field + "", Play.configuration.getString("form.hash.key").get.getBytes) match {
      case true => Some(true)
      case false => None
    }
  }

  def Authenticated(action: => Request[AnyContent] => SimpleResult) = Action {
    implicit request =>
      clientEmail match {
        case None => onUnauthorized
        case Some(email) => action(request)
      }
  }

  private def onUnauthorized = Ok(toJson(JsObject(List("msg" -> JsString("0")))))

  private implicit def clientEmail(implicit request: RequestHeader) = request.session.get("client")
}
