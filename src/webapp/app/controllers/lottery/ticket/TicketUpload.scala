package controllers.lottery.ticket

import play.api.mvc._
import views.html
import controllers.auth.Auth
import play.api.i18n.Messages
import exception.AppException
import play.api.libs.iteratee.Enumerator
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import utils._
import models.lottery.drawing.{Drawing => DrawingModel}

object TicketUpload extends Controller with Auth {

	def show(did: Long, hid: String) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(did, hid).fold(Ok(html.changed_param()))(ok =>
        DrawingModel.byId(did, UserFromSession).fold(Ok(html.changed_param()))(drawing =>
          Ok(html.lottery.ticket.upload_ticket(drawing, html.lottery.nav(), Messages.apply("ticketupload.info")))))
		}
	}

	def listByDrawing(did: Long, hash: String) = Authorized(Redakteur_OR_Freigeber) {
		implicit request => {
			Unchanged(did, hash).map {
				ok =>
					DrawingModel.publicById(did).map {
						drawing =>
							models.lottery.ticket.TicketUpload.listByDrawing(did).map {
								tlist =>
									val total = tlist.size.toString
									val totalAmount = Format.currency(tlist.map(_.amount.toLong).sum.toString)
									val groupedAmounts = tlist.groupBy(_.amount).toList.sortWith((a, b) => a._1.toLong < b._1.toLong).map {
										t => t._2.size + " x " + Format.currency(t._1) + ": " + Format.currency(t._2.map(_.amount.toLong).sum.toString)
									}.map(s => s"""["paragraph", "$s"]""").mkString(",")
									val pdf_name = drawing.dbase.drawingType.name + " vom " + drawing.date.fold("")(Format.date)
									val output = new ByteArrayOutputStream()
									val json = s"""[{"title":"$pdf_name", "size":"a4"},
									               ["paragraph", "$pdf_name"],
									               ["paragraph", "Losanzahl: $total"],
									               ["paragraph", "Gesamtgewinn: $totalAmount"],
									               ["paragraph", "Nach Gewinnsumme gruppiert:"],
									               $groupedAmounts,
									               ["table",{"header":["Losnummer","Gewinn"], "width":"100%","border":false,"cell-border":false},""" +
										tlist.sortBy(_.number).map(ticket => {
											val number = ticket.number
											val cur = Format.currency(ticket.amount)
											s"""["$number","$cur"]"""
										}).mkString(",") + "]]]"
                  clj_pdf.main.pdf(json, output)
									import play.api.libs.concurrent.Execution.Implicits._
									val fileContent: Enumerator[Array[Byte]] = Enumerator.fromStream(new ByteArrayInputStream(output.toByteArray))
									val agent = request.headers.get(USER_AGENT).getOrElse("empty")
									// if IE <= 8:
									val header = if(Matcher.is_IE_up_2_version_8(agent)) "attachment; filename=losnummern.pdf"
									// all other browsers (including IE >= 9
									else "attachment; filename*=UTF-8''losnummern.pdf"
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

	def upload(did: Long, hid: String) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(did, hid).map {
				ok =>
					var back = (false, "")
					request.body.asMultipartFormData.headOption.map {
						m => m.file("ticket_file").map {
							fp =>
								DrawingModel.byId(did, UserFromSession).map {
									drawing =>
										back = models.lottery.ticket.TicketUpload.add(drawing, fp) match {
											case Right(tickets) => (true, "")
											case Left(ex: AppException) => (false, ex.message)
										}
								}.getOrElse((false, Left(AppException)))
						}
					}
					val result = back._1 match {
						case false => JsonUtils.getStatus(status = false, back._2)
						case true => JsonUtils.getStatus(status = true, Messages.apply("file.upload.ok"))
					}
					Ok(result)
			} getOrElse Ok(html.changed_param())
		}
	}
}