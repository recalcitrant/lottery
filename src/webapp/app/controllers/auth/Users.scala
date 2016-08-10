package controllers.auth

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import views.html
import models.auth.UserConstants._
import utils.JsonUtils
import play.api.libs.json._
import play.api.libs.json.Json._
import models.auth.{User => UserModel}
import play.api.i18n.{Lang, Messages}

case class UpdateUser(id: Long,
                      hid: String,
                      username: String,
                      email: String,
                      password: String,
                      passwordrep: String,
                      lid: Long,
                      hlid: String,
                      selfedit: String,
                      permission: Option[Long])

case class InsertUser(username: String, email: String, password: String, passwordrep: String, permission: Long)

object Users extends Controller with Auth with UserNav {

  val updateForm = Form[UpdateUser](
    mapping(USER_ID -> longNumber, USER_HASHED_ID -> text, USER_NAME -> nonEmptyText(5), USER_EMAIL -> (email verifying nonEmpty), USER_PASSWORD -> text, USER_PASSWORD_REP -> text, USER_LOTTERY_ID -> longNumber, USER_HASHED_LOTTERY_ID -> text, USER_SELF_EDIT -> text, USER_PERMISSIONS -> optional(longNumber)
    )(UpdateUser.apply)(UpdateUser.unapply)
      verifying("password.minlength", result => result match {
      case (user: UpdateUser) =>
        if (!user.password.trim().isEmpty) 8 <= user.password.trim().length() && userNameOK(user.id, user.username)
        else userNameOK(user.id, user.username)
    })
      verifying("user.update.username.empty", result => result match {
      case (user: UpdateUser) => 5 <= user.username.trim().length()
    })
      verifying("user.update.email.empty", result => result match {
      case (user: UpdateUser) => 5 <= user.email.trim().length()
    })
      verifying("passwords.do.not.match", result => result match {
      case (user: UpdateUser) => user.password.trim() == user.passwordrep.trim()
    }))

  val insertForm = Form(
    mapping(
      USER_NAME -> nonEmptyText(5),
      USER_EMAIL -> (email verifying nonEmpty),
      USER_PASSWORD -> nonEmptyText(8),
      USER_PASSWORD_REP -> nonEmptyText(8),
      USER_PERMISSIONS -> longNumber
    )(InsertUser.apply)(InsertUser.unapply)
      verifying("error.username.exists", result => result match {
      case (user: InsertUser) => UserModel.byUsername(user.username).isEmpty
    })
      verifying("passwords.do.not.match", result => result match {
      case (user: InsertUser) => user.password.trim() == user.passwordrep.trim()
    }))

  def list(msg: String = "") = {
    Authorized(Admin) {
      implicit request =>
        Ok(html.user.list(UserModel.listByLottery(LotteryId), getNav(Some(user)), LotteryId, Messages.apply(msg)))
    }
  }

  def listByLottery(lid: Long, hlid: String, msg: String = "") = Authorized(Admin) {
    implicit request => {
      Unchanged(lid, hlid).map {
        ok =>
          Ok(html.user.list(UserModel.listByLottery(lid), getNav(Some(user)), lid, Messages.apply(msg)))
      } getOrElse Forbidden
    }
  }

  def addnew(lid: Long, hlid: String) = Authorized(Admin) {
    implicit request => {
      Unchanged(lid, hlid).map {
        ok =>
          Ok(html.user.add(insertForm, lid, hlid))
      }
    } getOrElse Forbidden
  }

  def add(lid: Long, hlid: String) = Authorized(Admin) {
    implicit request => {
      Unchanged(lid, hlid).map {
        ok =>
          insertForm.bindFromRequest().fold(
          errorForm => {
            Ok(toJson(Json.obj("status" -> JsString("!ok"), "errors" -> errorForm.errorsAsJson(Lang("de")))))
          }, {
            case (user: InsertUser) =>
              val result = UserModel.add(user, lid) match {
                case true => JsonUtils.getStatus(status = true)
                case _ => JsonUtils.getStatus(status = false, "user.add.failed")
              }
              Ok(result).withHeaders(CONTENT_TYPE_JSON)
          })
      }.getOrElse(Ok(toJson("!ok")))
    }
  }

  def get(id: Long, hash: String) = IsAuthenticated {
    implicit request =>
      Unchanged(id, hash).map {
        ok => UserModel.byId(id).map {
          user => {
            val perm: Long = user.permissions.headOption.map(_.id.get).getOrElse(-1)
            Ok(
              html.user.update(
                user.id.get,
                UserFromSession.hasEitherPermission(Seq(PERMISSION_ADMIN, PERMISSION_SUPER_ADMIN)),
                perm,
                updateForm.fill(
                  UpdateUser(user.id.get, user.hid, user.username, user.email, user.password, user.password, user.lottery.id.get, user.lottery.hid, "undefined", Option(perm)))))
          }
        } getOrElse NotFound
      } getOrElse Forbidden
  }

  def update(id: Long) = IsAuthenticated {
    implicit request =>
      updateForm.bindFromRequest().fold(
      errorForm => {
        Ok(toJson(JsObject(List("status" -> JsString("!ok"), "errors" -> errorForm.errorsAsJson(Lang("de"))))))
      }, {
        case (user: UpdateUser) =>
          Unchanged(id, user.hid).map {
            ok => {
	            UserModel.update(user, mayUpdate = true)
              if ("true" == user.selfedit) Ok(JsonUtils.getStatus(status = true)).withHeaders(CONTENT_TYPE_JSON).withSession(Security.username -> user.username.trim())
              else Ok(JsonUtils.getStatus(status = true)).withHeaders(CONTENT_TYPE_JSON)
            }
          } getOrElse Forbidden
      })
  }

  def delete(id: Long, hash: String) = Authorized(Admin) {
    implicit request =>
      Unchanged(id, hash).map {
        var result = JsonUtils.getStatus(status = false, "user.rm.failed")
        ok => {
          // do not delete myself:
          if (UserFromSession.id.get != id) {
            result = models.auth.User.delete(id) match {
              case 1 => JsonUtils.getStatus(status = true)
              case _ => JsonUtils.getStatus(status = false, "user.rm.failed")
            }
          }
          Ok(result).withHeaders(CONTENT_TYPE_JSON)
        }
      } getOrElse Forbidden
  }

  private def userNameOK(id: Long, username: String) =
	  UserModel.byUsername(username).map(user => user.id.get == id).getOrElse(true)
}