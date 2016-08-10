package controllers

import auth.Auth
import play.api.mvc._
import models.auth.{PermissionBundle, User}

object Index extends Controller with Auth {

  def index = IsAuthenticated {
    implicit request => {
      var url = controllers.auth.routes.Users.list("")
      User.byUsername(username.getOrElse("undefined")).map {
        u => u.permissions.foreach {
          p => {
            if (p.id.get == PERMISSION_REDAKTEUR || p.id.get == PERMISSION_FREIGEBER)
              url = controllers.lottery.drawing.routes.Drawing.list("")
          }
        }
        Results.Redirect(url)
      } getOrElse Forbidden
    }
  }
}
