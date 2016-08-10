package controllers.auth

import views.html
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.auth.User
import play.api.i18n.Messages

object Authentication extends Controller with Auth {

  val loginForm = Form(
    tuple(
      "username" -> nonEmptyText(5, 255),
      "password" -> nonEmptyText(8, 255),
      "returnUrl" -> nonEmptyText
    ) verifying(Messages.apply("wrong.username.or.password"),
      user => User.authenticate(user._1, user._2.trim()).isDefined
    ))

  def login = Action(implicit request => {
    Ok(html.user.login(loginForm, returnUrl))
  })

  def logout = Action {
    implicit request =>
      Redirect(routes.Authentication.login()).withNewSession.flashing(
        "success" -> "Sie haben sich erfolgreich abgemeldet")
  }

  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest().fold(
      errors => {
        BadRequest(html.user.login(errors, returnUrl))
      }, {
        case (username, pw, returl) =>
          User.resetMaxLogins(username.trim())
          Redirect(returl).withSession(session + (Security.username -> username.trim()))
      })
  }
}