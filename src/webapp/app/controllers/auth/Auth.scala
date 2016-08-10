package controllers.auth

import play.api.mvc._
import play.api.mvc.Results._
import utils.JsonUtils
import play.api.i18n.Messages
import play.api.libs.Crypto
import play.api.Play
import play.api.Play.current
import models.auth.{PermissionBundle, User, Permission}
import controllers.CTypes
import scala.concurrent.Future

trait Auth extends CTypes with PermissionBundle {

  implicit def LotteryId(implicit request: RequestHeader) = User.byUsername(username.getOrElse("undefined")).get.lottery.id.get

  implicit def LotteryType(implicit request: RequestHeader) = User.byUsername(username.getOrElse("undefined")).get.lottery.lotteryType.id.get

  implicit def UserFromSession(implicit request: RequestHeader) = User.byUsername(username.getOrElse("undefined")).get

  def Unchanged(field: Long, hash: String) = {
    hash == Crypto.sign(field + "", Play.configuration.getString("form.hash.key").get.getBytes) match {
      case true => Some(true)
      case false => None
    }
  }

  def Authorized(perms: Seq[Long])(action: => Request[AnyContent] => SimpleResult) = IsAuthenticated {
    implicit request =>
      User.byActiveUsername(username.getOrElse("undefined")).map {
        u =>
          perms.map(id => Permission.byId(id)).exists(p => u.hasPermissionOrIsSuperAdmin(p.id.get)) match {
            case true => action(request)
            case false => Results.Forbidden
          }
      }.getOrElse(Results.Forbidden)
  }

  def IsAuthenticated(action: => Request[AnyContent] => SimpleResult) = Action {
    implicit request =>
      username match {
        case None => onUnauthorized(request)
        case Some(name) => action(request)
      }
  }

  private def onUnauthorized(request: RequestHeader) = Results.Redirect {
    routes.Authentication.login().url + "?returnUrl=" + encodeUrl(request.path)
  }

  protected def user(implicit request: RequestHeader) = username(request).getOrElse("undefined")

  protected implicit def username(implicit request: RequestHeader) = request.session.get("username")

  protected def encodeUrl = java.net.URLEncoder.encode(_: String, "UTF-8")

  protected def returnUrl(implicit request: RequestHeader) =
    request.queryString.get("returnUrl").map(_.head).getOrElse(controllers.routes.Index.index().url)

  def isSuperAdmin(request: RequestHeader) =
    User.byUsername(request.session.get("username").getOrElse("undefined")).exists(u => u.hasPermission(PERMISSION_SUPER_ADMIN))

  def TamperedWithParam = Ok(JsonUtils.getStatus(status = false, Messages.apply("param.tampered.with"))).withHeaders(CONTENT_TYPE_JSON)
}
