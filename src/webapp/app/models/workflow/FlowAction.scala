package models.workflow

import scala.xml._

import collection.mutable.ArrayBuffer
import models.auth.User
import models.workflow.WorkflowConstants._
import javax.mail.internet.InternetAddress
import models.lottery.drawing.Drawing
import utils.mail.Mailer
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

case class FlowAction(id: String, name: String, did: Long, state: State, user: User, wdef: String) {

  val doc = XML.loadString(wdef)

  def exec(lid: Long, mailComment: String, user:User) = {
    if (!checkRoles) false //Left(AppException(ExceptionCodes.ROLE_IS_MISSING))
    else {
      val newstateid = ((doc \ TRANSITIONS \ TRANSITION).filter(stat => stat \ ATTR_FROM contains Text(state.id.toString)).filter(stat => stat \ ATTR_ACTION contains Text(id)) \ ATTR_TO text).toLong
      val newstate = State(newstateid, did, State.name4Id(newstateid).getOrElse("Unbekannt"))
      State.update(newstate, user) exists {
	      rows =>
		      Future {
			      sendMails(lid, newstate.name, mailComment)
		      }
		      true
      }
    }
  }


  private def checkRoles = {
    var back = false
    XML.loadString(wdef) \ TRANSITIONS \ TRANSITION filter (_ \ ATTR_FROM contains Text(state.id.toString)) foreach (transition => {
      if (id == (transition \ ATTR_ACTION text)) {
        val act = doc \ ACTIONS \ ACTION filter (_ \ ATTR_ID contains Text(id))
        val required_roles = for (role <- act \ ROLES \ ROLE) yield (role \ ATTR_NAME).text
        if (0 == required_roles.size || hasRole(required_roles)) back = true
      }
    })
    back
  }

  private def sendMails(lid: Long, newstate: String, mailComment: String) {
    val act = doc \ ACTIONS \ ACTION filter (_ \ ATTR_ID contains Text(id))
    act \ NOTIFICATIONS \ ROLE foreach (rle => {
      val role = (rle \ ATTR_NAME text).toLong
      Mailer.workflowAction(Drawing.publicById(did), newstate, mailComment, User.listByLotteryAndPermission(lid, role).map(u => new InternetAddress(u.email)))
    })
  }

  def hasRole(requiredRoles: Seq[String]) = requiredRoles exists (r => user.hasPermission(r.toLong))
}

object FlowAction {


  def getActions(did: Long, state: State, user: User, wdef: String) = {
    val actions = ArrayBuffer[FlowAction]()
    val doc = XML.loadString(wdef)
    doc \ TRANSITIONS \ TRANSITION filter (_ \ ATTR_FROM contains Text(state.id.toString)) foreach {
      transition => {
        doc \ ACTIONS \ ACTION filter (_ \ ATTR_ID contains Text(transition \ ATTR_ACTION text)) foreach {
          action => {
            val required_roles = for (role <- action \ ROLES \ ROLE) yield (role \ ATTR_NAME).text
            val id = (action \ ATTR_ID).text
            val name = (action \ ATTR_NAME).text
            val act = FlowAction(id, name, did, state, user, wdef)
            if (0 == required_roles.size || act.hasRole(required_roles))
              actions += act
          }
        }
      }
    }
    actions
  }
}