package controllers.client

import play.api.mvc._
import controllers.auth.Auth
import play.api.libs.iteratee.Enumerator
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import utils._

object NotificationHistory extends Controller with Auth {

  def listByDrawing(did: Long, hash: String) = Authorized(Redakteur_OR_Freigeber) {
    implicit request => {
      Unchanged(did, hash).map {
        ok =>
          models.lottery.drawing.Drawing.publicById(did).map {
            drawing =>
              models.client.NotificationHistory.showHistory(did).map {
                hlist =>
                  val pdf_name = drawing.dbase.drawingType.name + " vom " + drawing.date.fold("")(Format.date)
                  val output = new ByteArrayOutputStream()
                  val total_count = hlist.size
                  val total_text = s"Es wurden $total_count E-Mails an den Versand übergeben."
                  val json = s"""[{"title":"$pdf_name", "size":"a4"},
																 ["paragraph", "$pdf_name"],
                                 ["paragraph", "$total_text"],
																 ["table",
																 {
																   "header":["Vorname","Nachname", "E-Mail","Gewinn","Versanddatum"],
																   "width":"100%","border":false,"cell-border":false
																 },""" +
                    hlist.map(hist => {
                      val first = hist.firstname
                      val last = hist.lastname
                      val recip = hist.recipient
                      val cur = Format.currency(hist.amount)
                      val dt = Format.dateWithTime(hist.dateSent)
                      s"""["$first","$last","$recip","$cur", "$dt"]"""
                    }).mkString(",") + "]]]"
                  clj_pdf.main.pdf(json, output)
                  import play.api.libs.concurrent.Execution.Implicits._
                  val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(new ByteArrayInputStream(output.toByteArray))
                  val agent = request.headers.get(USER_AGENT).getOrElse("empty")
                  val fileName = "mailversand_" + drawing.dbase.drawingType.name + "_vom_" + drawing.date.fold("")(Format.date) + ".pdf"
                  // if IE <= 8:
                  val header = if (Matcher.is_IE_up_2_version_8(agent)) "attachment; filename=" + fileName
                  // all other browsers (including IE >= 9
                  else "attachment; filename*=UTF-8''" + fileName
                  SimpleResult(
                    header = ResponseHeader(200, Map(
                      CONTENT_DISPOSITION -> header,
                      CONTENT_LENGTH -> output.size().toString,
                      CONTENT_TYPE -> CONTENT_TYPE_PDF)),
                    body = fileContent
                  )
              } getOrElse NotFound
          } getOrElse NotFound
      } getOrElse Forbidden
    }
  }
}