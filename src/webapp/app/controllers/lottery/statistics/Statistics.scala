package controllers.lottery.statistics

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.{Locale, Date}

import controllers.auth.Auth
import models.lottery.Branch
import models.lottery.statistics.{Statistics => StatisticsModel, Stats, StatisticsType}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.iteratee.Enumerator
import utils.{Matcher, Format}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.mvc._

object Statistics extends Controller with Auth {

  def get = Authorized(Redakteur_OR_Freigeber) {
    implicit request => {
      val lottery = UserFromSession.lottery
      val overviewDrawings = StatisticsModel.getByLidDrawings(lottery.id.get, StatisticsType.TYPE_OVERVIEW)
      val queries = StatisticsModel.getByLidDrawings(lottery.id.get, StatisticsType.TYPE_QUERY)
      val addedUsersByLottery = StatisticsModel.getAddedUsersByLottery(lottery.id.get)
      val unregisteredUsersByLottery = StatisticsModel.getUnregisteredUsersByLottery(lottery.id.get)
      val branches = Branch.allByLottery(lottery.id.get)
      val output = new ByteArrayOutputStream()
      val now = Format.date(new Date)

      def filterList(list: List[String]) = if (list.nonEmpty) list.filter(!_.equals("")).mkString(",") else ""

      def filter(s: String) = if (s.nonEmpty) "," + s.filter(!_.equals("")) else ""

      def convert(stats: List[Stats], headline: String) = {
        if (stats.nonEmpty) {
          headline + stats.map(stat => {
            val year = stat.year
            val dt = new DateTime(stat.year, stat.month, 1, 1, 1)
            val month = dt.toString(DateTimeFormat.forPattern("MMMM").withLocale(Locale.GERMANY))
            val count = stat.count
            s"""["$year","$month","$count"]"""
          }).mkString(",") + "]\n"
        } else ""
      }

      def convertByBranch(f: (Long) => List[Stats], headline: String) = {
        branches.map {
          branch =>
            val name = branch.name
            val code = branch.code
            val stats = f(code)
            convert(stats, s"""["paragraph", "$headline der $name ($code)"],["table",""")
        }.filter(!_.isEmpty)
      }

      val addedUsersByLotteryJSON = convert(addedUsersByLottery, """["paragraph", "Kundenzugänge der Lotterie:"], ["table",""")
      val unregisterdUsersByLotteryJSON = convert(unregisteredUsersByLottery, """["paragraph", "Kundenabgänge der Lotterie:"], ["table",""")
      val addedUsersByBranchAndMonthJSON = convertByBranch((code) => StatisticsModel.getAddedUsersByBranch(code), "Kundenzugänge")
      val unregisteredUsersByBranchAndMonthJSON = convertByBranch((code) => StatisticsModel.getUnregisteredUsersByBranch(code), "Kundenabgänge")
      val queriesByBranchJSON = convertByBranch((code) => StatisticsModel.getByBranchCodeDrawings(code, StatisticsType.TYPE_QUERY), "Aufrufe der Gewinnabfrage")
      val overviewByBranchJSON = convertByBranch((code) => StatisticsModel.getByBranchCodeDrawings(code, StatisticsType.TYPE_OVERVIEW), "Aufrufe der Gewinnübersicht")

      val combinedJson =
        convert(overviewDrawings, s"""[{"title":"Statistik vom $now", "size":"a4"},["paragraph", "Statistik vom $now"],["paragraph", "Aufrufe der Gewinnübersicht gesamt:"],["table",""") +
          convert(queries, """,["paragraph", "Aufrufe der Gewinnabfragen gesamt:"],["table",""") + "," +
          filterList(overviewByBranchJSON) +
          filterList(queriesByBranchJSON) +
          filter(addedUsersByLotteryJSON) + (if(addedUsersByLotteryJSON.nonEmpty) "," else "") +
          filterList(addedUsersByBranchAndMonthJSON) +
          filter(unregisterdUsersByLotteryJSON) + (if(unregisterdUsersByLotteryJSON.nonEmpty) "," else "") +
          filterList(unregisteredUsersByBranchAndMonthJSON) + "]"

      clj_pdf.main.pdf(combinedJson, output)
      import play.api.libs.concurrent.Execution.Implicits._
      val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(new ByteArrayInputStream(output.toByteArray))
      val agent = request.headers.get(USER_AGENT).getOrElse("empty")
      val fileName = "statistik_vom_" + now + ".pdf"
      // if IE <= 8:
      val header = if (Matcher.is_IE_up_2_version_8(agent)) "attachment; filename=" + fileName
      // all other browsers (including IE >= 9
      else "attachment; filename*=UTF-8''" + fileName
      SimpleResult(
        header = ResponseHeader(200, Map(CONTENT_DISPOSITION -> header, CONTENT_LENGTH -> output.size().toString, CONTENT_TYPE -> CONTENT_TYPE_PDF)),
        body = fileContent)
    }
  }
}
