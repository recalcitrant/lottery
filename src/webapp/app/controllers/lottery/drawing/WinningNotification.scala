package controllers.lottery.drawing

import play.api.i18n.Messages
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json._
import play.api.mvc._
import views.html
import controllers.auth.Auth
import utils.{Matcher, JsonUtils}
import models.lottery.drawing.{WinningNotification => WinningNotificationModel}
import models.lottery.drawing.{Drawing => DrawingModel}

object WinningNotification extends Controller with Auth {

	private val validUploadFileTypes = Seq("jpg", "jpeg", "gif", "png")

	private def retreiveFile(implicit request: Request[AnyContent], lid: Long, did: Long) = {
		val res = WinningNotificationModel.fileByDrawingId(lid, did)
		res.map {
			case (name, file) =>
				import play.api.libs.concurrent.Execution.Implicits._
				val fileContent: Enumerator[Array[Byte]] = Enumerator.fromFile(file)
				val suffix = name.substring(name.lastIndexOf(".") + 1).toLowerCase
				val agent = request.headers.get(USER_AGENT).getOrElse("empty")
				val ctype =
					if (validUploadFileTypes.contains(suffix)) if (Matcher.isIE(agent)) CONTENT_TYPE_PJPEG else CONTENT_TYPE_JPG
					else CONTENT_TYPE_BINARY
				val headers = Map(CONTENT_LENGTH -> file.length.toString, CONTENT_TYPE -> ctype)
				SimpleResult(
					header = ResponseHeader(200, headers),
					body = fileContent
				)
		} getOrElse NotFound
	}

	def upload(did: Long) = Authorized(Redakteur) {
		implicit request => {
			var back = false
			request.body.asMultipartFormData.headOption.map {
				m => m.file("notification_file").map {
					fp => {
						back = {
							val suffix = fp.filename.substring(fp.filename.lastIndexOf(".") + 1).toLowerCase
							if (validUploadFileTypes.exists(vf => vf.toLowerCase == suffix)) WinningNotificationModel.upload(LotteryId, did, fp).getOrElse(false)
							else false
						}
					}
				}
			}
			val result = back match {
				case false => JsonUtils.getStatus(status = false, Messages.apply("file.upload.notok"))
				case true => JsonUtils.getStatus(status = true, Messages.apply("file.upload.ok"))
			}
			Ok(result)
		}
	}

	def deleteUpload(did: Long) = Authorized(Redakteur) {
		implicit request => {
			val result = WinningNotificationModel.deleteUpload(LotteryId, did).exists(1 ==)
			Ok(JsonUtils.getStatus(result, if (result) "prize.upload.rm.ok" else "prize.upload.rm.notok")).withHeaders(CONTENT_TYPE_JSON)
		}
	}

	def getDescription(id: Long) = Authorized(Redakteur) {
		implicit request =>
			Ok(toJson(WinningNotificationModel.getDescription(id).getOrElse("")))
	}

	def getImage(lid:Long, did: Long) = Action {
		implicit request => {
			retreiveFile(request, lid, did)
		}
	}

	def deleteImage(nid: Long, hid: String) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(nid, hid).map {
				ok => Forbidden
			}
		} getOrElse Forbidden
	}

	def updateDescription(did: Long, hid: String) = Authorized(Redakteur) {
		implicit request => {
			Unchanged(did, hid).map {
				ok =>
					val desc = request.body.asJson.map(_.as[String]).getOrElse("")
					val result = WinningNotificationModel.updateDescription(did, desc) match {
						case Some(i) => JsonUtils.getStatus(status = true, Messages.apply("prize.description.update.ok"))
						case None => JsonUtils.getStatus(status = false, Messages.apply("prize.description.update.notok"))
					}
					Ok(result).withHeaders(CONTENT_TYPE_JSON)
			}
		} getOrElse Forbidden
	}

	def getUploadForm(did: Long) = Authorized(Redakteur) {
		implicit request => {
			Ok(html.lottery.winningnotification.upload_winning_notification_image(did))
		}
	}


	/*def count(nid: Long) = Authorized(Redakteur) {
		implicit request => {
			val count = WinningNotificationModel.count(nid).getOrElse(0l)
			Ok(toJson(JsObject(List("count" -> JsString(count.toString)))))
		}
	}*/

	def get(did: Long, hid: String, msg: String = "") = Authorized(Redakteur) {
		implicit request => {
			Unchanged(did, hid).map {
				ok =>
					DrawingModel.byId(did, UserFromSession).map {
						drawing =>
							Ok(html.lottery.winningnotification.winning_notification_content(drawing, html.lottery.nav()))
					} getOrElse NotFound
			} getOrElse Forbidden
		}
	}

	/*def get(id: Long) = Authorized(Redakteur_OR_Freigeber) {
		implicit request =>
			WinningNotificationModel.byId(id, UserFromSession).map {
				drawing =>
					Ok(html.lottery.drawing.show_drawing(drawing))
			} getOrElse NotFound
	}*/
}
