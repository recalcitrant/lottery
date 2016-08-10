package controllers.client

import controllers.DBAction
import models.client.UserQuery
import models.lottery.drawing.Drawing
import models.lottery.prize.{Prize, PrizeUpload}
import models.lottery.statistics.{Statistics, StatisticsType}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc._
import utils.Config

import scala.concurrent.{ExecutionContext, Future}

object PublicDrawing extends Controller with DBAction {

  implicit val dbExcon: ExecutionContext = Akka.system.dispatchers.lookup("play.akka.actor.db")

  def latestByBranch(bcode: String) = Action.async {
    val future = Drawing.listLatestByBranch(bcode)
    future.map(drawingOpts => {
      Future(Statistics.add(bcode, StatisticsType.TYPE_OVERVIEW))
      Ok(drawingOpts.map(toJson(_)) getOrElse JsArray())
    })
  }

  def query(did: Long) = Action {
    implicit request => {
      Drawing.publicById(did).map {
        drawing => {
          request.body.asJson.map {
            json => {
              val result = Drawing.userQuery(
                drawing.id.get,
                drawing.dbase.lottery.lotteryType.id.get,
                json.validate[UserQuery].get)
              Ok(toJson(result))
            }
          } getOrElse NotFound
        }
      } getOrElse NotFound
    }
  }

  def queryByDrawingAndBranch(did: Long, bcode: String) = Action {
    implicit request => {
      Drawing.publicById(did).map {
        drawing => {
          Statistics.add(bcode, StatisticsType.TYPE_QUERY)
          request.body.asJson.map {
            json => {
              val result = Drawing.userQuery(
                drawing.id.get,
                drawing.dbase.lottery.lotteryType.id.get,
                json.validate[UserQuery].get)
              Ok(toJson(result))
            }
          } getOrElse NotFound
        }
      } getOrElse NotFound
    }
  }

  def prize(id: Long, bcode: String) = Action {

    def foldUploads(bcode: String, uploads: Seq[PrizeUpload]) = {
      uploads.foldLeft(JsArray()) {
        (acc, item) => {
          val url = Config.getString("server.url") + controllers.lottery.prize.routes.PrizeUpload.showFile(item.id.get, bcode).url
          acc :+ obj("name" -> item.url, "url" -> url)
        }
      }
    }

    Ok(toJson(
      Prize.byId(id).map {
        prize => {
          obj(
            "title" -> JsString(prize.title.getOrElse("")),
            "desc" -> JsString(prize.description.getOrElse("")),
            "uploads" -> foldUploads(bcode, prize.uploads))
        }
      }.getOrElse(JsObject(Nil))))
  }
}