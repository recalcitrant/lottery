package controllers.auth

import views.html
import play.api.mvc.Controller
import models.auth.{PermissionBundle, User}
import play.api.templates.Html

trait UserNav extends Controller with PermissionBundle {

  def getNav(username: Option[String]): Html =
    html.user.nav(User.byUsername(username.getOrElse("undefined")).exists(u => u.hasPermission(PERMISSION_SUPER_ADMIN)))
}