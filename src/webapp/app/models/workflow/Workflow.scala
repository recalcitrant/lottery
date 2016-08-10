package models.workflow

import play.api.Play.current
import anorm.SqlParser._
import anorm._
import play.api.db.DB
import models.auth.User
import models.lottery.drawing.Drawing
import java.util.Date

case class Workflow(did: Long, state: State, user: User, wdef: String) {

  def getActions = FlowAction.getActions(did, state, user, wdef)

  def action(action: String, mailComment: String, oldState: Long, lid: Long, user: User) = {
    if (oldState == state.id) FlowAction(action, "", did, state, user, wdef).exec(lid, mailComment, user)
    else false
  }
}

case class WorkflowHistory(email: String, state: State, updated: Date, drawing: Drawing)

object Workflow {

  val STATE_ZUR_FREIGABE_VORGELEGT = 1
  val STATE_ZURUECKGEWIESEN = 2
  val STATE_FREIGEGEBEN = 3
  val STATE_OFFEN = 4
  val ACTION_FREIGEBEN = "freigeben"

  val sqlByDrawing = "SELECT * FROM workflow_drawing wd, workflow_status ws, workflow_definition wdef WHERE wd.drawing_id = {did} AND wd.workflow_status_id = ws.id"
  val sqlHistory = "SELECT * FROM workflow_history wh, workflow_status ws where drawing_id = {did} AND wh.workflow_status_id = ws.id ORDER BY wh.id desc"

  def byDrawing(did: Long, user: User) = DB.withConnection(implicit con =>
    SQL(sqlByDrawing).on("did" -> did).as(Workflow.instance(user).singleOpt))

  def history(did: Long) = DB.withConnection(implicit con =>
    Drawing.publicById(did) map {
      drawing =>
        SQL(sqlHistory).on("did" -> did).as(Workflow.historyInstance(drawing) *)
    } getOrElse Seq[WorkflowHistory]())

  private def instance(user: User) = get[Long]("workflow_drawing.drawing_id") ~ get[Long]("workflow_drawing.workflow_status_id") ~ get[String]("workflow_status.name") ~ get[String]("workflow_definition.value") map {
    case did ~ sid ~ name ~ wdef => Workflow(did, State(sid, did, name), user, wdef)
  }

  private def historyInstance(drawing: Drawing) =
    get[String]("workflow_history.email") ~ get[Long]("workflow_history.workflow_status_id") ~
      get[Date]("workflow_history.updated") ~ get[String]("workflow_status.name") map {
      case mail ~ sid ~ updated ~ sname => WorkflowHistory(mail, State(sid, drawing.id.get, sname), updated, drawing)
    }
}