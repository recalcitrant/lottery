package models.lottery.drawing

import models.lottery.ticket.Ticket
import models.auth.User
import models.workflow.Workflow
import models.lottery.prize.PrizeCount

import anorm.SqlParser._
import anorm._
import anorm.Pk
import java.util.Date
import models.lottery.dbase.DrawingBase
import models.client.UserQuery

object DrawingSQL {

  private[drawing] val sql = "SELECT * FROM drawing d, dbase db where d.drawing_base_id = db.id"
  private[drawing] val sqlUpdate = "UPDATE drawing SET drawing_date = {drawing_date}, drawing_date_next = {drawing_date_next} , date_winning_notification = {date_winning_notification}, date_publish = {date_publish} WHERE id = {id}"
  private[drawing] val sqlDelete = "DELETE FROM drawing WHERE id = {id}"
  private[drawing] val sqlInsert = "INSERT INTO drawing ( drawing_date, drawing_date_next, date_winning_notification, date_publish, drawing_base_id ) VALUES ( {drawing_date}, {drawing_date_next}, {date_winning_notification}, {date_publish}, {drawing_base_id} )"
  private[drawing] val sqlWorkflowInsert = "INSERT INTO workflow_drawing ( drawing_id, workflow_status_id ) VALUES ( {did}, {wsid} )"

  private[drawing] val sqlPublicQuery = """ SELECT p.id, p.typ, p.value, p.title, dfd.digits, dfd.date_winning_notification
                                            FROM drawing_finaldigit dfd, drawing_prize dp, dbase_prize p
                                            WHERE dfd.drawing_prize_id = dp.id
                                            AND dp.prize_id = p.id
                                            AND dp.drawing_id = {id}"""

  private[drawing] val sqlByBranch = """SELECT d.id, typ, drawing_date, drawing_date_next, date_winning_notification, date_publish, drawing_base_id
                                      FROM drawing d, dbase db, lottery l, branch b, workflow_drawing wd
                                      WHERE d.drawing_base_id = db.id
                                      AND wd.drawing_id = d.id
                                      AND wd.workflow_status_id = 3
                                      AND b.lottery_id = l.id
                                      AND db.lottery_id = b.lottery_id
                                      AND b.code = {bcode}
                                      ORDER BY d.drawing_date desc
                                      LIMIT 0,{drawings_visible}"""

	private[drawing] val sqlByOLD_WHVN = """SELECT d.id, typ, drawing_date, drawing_date_next, date_winning_notification, date_publish, drawing_base_id
	                                        FROM drawing d, dbase db, branch b, workflow_drawing wd
	                                        WHERE d.drawing_base_id = db.id
	                                        AND wd.drawing_id = d.id
	                                        AND wd.workflow_status_id = 3
	                                        AND db.lottery_id = 106
	                                        AND b.code = 28250110
	                                        ORDER BY d.drawing_date desc
                                          LIMIT 0,{drawings_visible}"""

  private[drawing] val byid = " AND d.id = {id} "
  private[drawing] val bylid = " AND db.lottery_id= {lid}  "
  private[drawing] val order = " ORDER BY d.drawing_date desc"

  private def flow(did: Long, user: User) = Workflow.byDrawing(did, user)

  private def dprize(did: Long) = DrawingPrize.byDrawing(did)

  private def pcount(did: Long) = Option(PrizeCount.byDrawing(did))

  private[drawing] def getTickets(did: Long) = Option(Ticket.byDrawing(did))

  //private[drawing] def getTickets(did: Long, tickets: Seq[String]) = Option(Ticket.byDrawingAndTickets(did, tickets))

  private[drawing] def byDrawingAndTicketRanges(did: Long, query: UserQuery) = Ticket.byDrawingAndTicketRanges(did, query)

  /*private[drawing] def id_type_date = get[Pk[Long]]("drawing.id") ~ get[Long]("dbase.typ") ~ get[Option[Date]]("drawing.drawing_date") map {
    case id ~ typ ~ date => Option(id.get.toString, typ.toString, date.map(DateUtils.date2Str(_)).getOrElse("Keine Angabe"))
  }*/

  private[drawing] def publicData = get[Long]("dbase_prize.id") ~ get[Long]("dbase_prize.typ") ~ get[String]("drawing_finaldigit.digits") ~ get[String]("dbase_prize.value") ~ get[Option[String]]("dbase_prize.title") ~ get[Option[Date]]("drawing_finaldigit.date_winning_notification") map {
    case pid ~ ptype ~ digits ~ pvalue ~ title ~ notification_date => (digits, DrawingResult(pid, pvalue, ptype, title, -1, TicketDigit(NotAssigned, digits, notification_date), DrawingResult.TYPE_DIGITS))
  }

  private[drawing] def publicInstance = get[Pk[Long]]("drawing.id") ~ get[Option[Date]]("drawing.drawing_date") ~ get[Option[Date]]("drawing.drawing_date_next") ~ get[Option[Date]]("drawing.date_winning_notification") ~ get[Option[Date]]("drawing.date_publish") ~ get[Long]("drawing_base_id") map {
    case id ~ created ~ next ~ displayDate ~ datePublish ~ dbid => Drawing(id, DrawingBase.byId(dbid).get, dprize(id.get), pcount(id.get), None, None, WinningNotification.byDrawing(id.get), created, next, datePublish, displayDate)
  }

  private[drawing] def instance(user: User) = get[Pk[Long]]("drawing.id") ~ get[Option[Date]]("drawing.drawing_date") ~ get[Option[Date]]("drawing.drawing_date_next") ~ get[Option[Date]]("drawing.date_winning_notification") ~ get[Option[Date]]("drawing.date_publish") ~ get[Long]("drawing_base_id") map {
    case id ~ created ~ next ~ displayDate ~ datePublish ~ dbid => Drawing(id, DrawingBase.byId(dbid).get, dprize(id.get), pcount(id.get), getTickets(id.get), flow(id.get, user), WinningNotification.byDrawing(id.get), created, next, datePublish, displayDate)
  }
}
