package models.workflow

import play.api.db.DB
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import models.auth.User
import java.util.Date

case class State(id: Long, did: Long, name: String)

object State {

  val sql = "select * from workflow_status ws"

  def update(state: State, user: User) = {
    DB.withTransaction {
      implicit con =>
        val rows = SQL("update workflow_drawing set workflow_status_id = {newid} where drawing_id = {did}").onParams(state.id, state.did).executeUpdate()
        if (0 == rows) None
        else {
          SQL("insert into workflow_history (email, workflow_status_id, drawing_id, updated) values ({mail}, {newid}, {did}, {timestamp})").onParams(user.email, state.id, state.did, new Date()).executeUpdate()
          Some(rows)
        }
    }
  }

  def name4Id(id: Long) = DB.withConnection {
    implicit con => SQL("select name from workflow_status where id = {id}").onParams(id).as(scalar[String].singleOpt)
  }
}