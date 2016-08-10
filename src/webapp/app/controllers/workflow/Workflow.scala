package controllers.workflow

import play.api.mvc._
import views.html
import controllers.auth.Auth
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

object Workflow extends Controller with Auth {

  def get(did: Long, hid: String) = Authorized(Redakteur_OR_Freigeber) {
    implicit request => {
      Unchanged(did, hid).map {
        ok =>
          Ok(html.workflow.show_workflow(models.workflow.Workflow.byDrawing(did, UserFromSession).get, html.lottery.nav()))
      }.getOrElse(Ok(html.changed_param()))
    }
  }

  def history(did: Long, hid: String) = Authorized(Redakteur_OR_Freigeber) {
    implicit request => {
      Unchanged(did, hid).map {
        ok =>
          Ok(html.workflow.history(models.workflow.Workflow.history(did)))
      }.getOrElse(Ok(html.changed_param()))
    }
  }

  def action(did: Long, hid: String, oldState: Long) = Authorized(Redakteur_OR_Freigeber) {
    implicit request => {
      Unchanged(did, hid).map {
        ok =>
          models.lottery.drawing.Drawing.publicById(did).map {
            drawing =>
              request.body.asJson.map {
                json => {
                  val action = (json \ "action").as[String]
                  val emailComment = (json \ "emailcomment").as[String]
                  val result = models.workflow.Workflow.byDrawing(did, UserFromSession).map(flow => flow.action(action, emailComment, oldState, LotteryId, UserFromSession)).exists {
                    flowActionOK =>
                      if (flowActionOK && models.workflow.Workflow.ACTION_FREIGEBEN == action) {
                        models.lottery.Lottery.byId(drawing.dbase.lottery.id.get).map {
                          lottery =>
                            Future {
                              models.client.Client.addToWinningNotificationQueue(models.client.Client.listByLotteryAndBranchesWithActiveNotification(lottery.id.get), drawing)
                            }
                        }
                      }
                      flowActionOK
                  }
                  if (result) Ok(JsObject(List("status" -> JsString("ok"), "msg" -> JsString(play.api.i18n.Messages.apply("workflow.action.ok")))))
                  else Ok(JsObject(List("status" -> JsString("!ok"), "msg" -> JsString(play.api.i18n.Messages.apply("workflow.action.notok")))))
                }
              } getOrElse Forbidden
          } getOrElse NotFound
      } getOrElse Forbidden
    }
  }
}