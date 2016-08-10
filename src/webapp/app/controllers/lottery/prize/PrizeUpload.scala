package controllers.lottery.prize

import play.api.mvc._
import views.html
import controllers.auth.Auth
import play.api.i18n.Messages
import java.net.URLEncoder
import play.api.libs.iteratee.Enumerator
import utils.{Matcher, StringUtils, JsonUtils}
import play.api.libs.json._
import play.api.libs.json.Json._
import models.lottery.prize.{PrizeUpload => UploadModel}

object PrizeUpload extends Controller with Auth {

  private val validUploadFileTypes = Seq("jpg", "jpeg", "pdf")

  def asAttachment(id: Long, hash: String) = Authorized(Redakteur) {
    implicit request => {
      Unchanged(id, hash).map {
        ok =>
          val res = UploadModel.fileById(LotteryId, id)
          res.map {
            case (filename, file) => {
	            import play.api.libs.concurrent.Execution.Implicits._
              val fileContent: Enumerator[Array[Byte]] = Enumerator.fromFile(file)
              val result = StringUtils.encode4Download(URLEncoder.encode, filename)
              val agent = request.headers.get(USER_AGENT).getOrElse("empty")
              // if IE <= 8:
              val attachment = if (Matcher.is_IE_up_2_version_8(agent)) "attachment; filename=" + result
              // all other browsers (including IE >= 9
              else "attachment; filename*=UTF-8''" + result
              SimpleResult(
                header = ResponseHeader(200, Map(
                  CONTENT_DISPOSITION -> attachment,
                  CONTENT_LENGTH -> file.length.toString,
                  CONTENT_TYPE -> CONTENT_TYPE_BINARY)),
                body = fileContent
              )
            }
          } getOrElse NotFound
      } getOrElse Forbidden
    }
  }

  private def retreiveFile(implicit request: Request[AnyContent], lid: Long, id: Long) = {
    val res = UploadModel.fileById(lid, id)
    res.map {
      case (name, file) => {
	      import play.api.libs.concurrent.Execution.Implicits._
        val fileContent: Enumerator[Array[Byte]] = Enumerator.fromFile(file)
        val suffix = name.substring(name.lastIndexOf(".") + 1)
        val isPDF = "pdf" == suffix
        // if IE <= 8:
        val agent = request.headers.get(USER_AGENT).getOrElse("empty")
        val attachment = if (Matcher.is_IE_up_2_version_8(agent)) "attachment; filename=" + name
        // all other browsers (including IE >= 9
        else "attachment; filename*=UTF-8''" + name
        val ctype =
          if (validUploadFileTypes.contains(suffix)) if (isPDF) CONTENT_TYPE_PDF else if (Matcher.isIE(agent)) CONTENT_TYPE_PJPEG else CONTENT_TYPE_JPG
          else CONTENT_TYPE_BINARY
        val headers = Map(
          CONTENT_LENGTH -> file.length.toString,
          CONTENT_TYPE -> ctype) ++
          (if (isPDF) Map(CONTENT_DISPOSITION -> attachment)
          else Map.empty)
        SimpleResult(
          header = ResponseHeader(200, headers),
          body = fileContent
        )
      }
    } getOrElse NotFound
  }

  def showFileByLid(id: Long, lid: Long) = Authorized(Redakteur_OR_Freigeber) {
    implicit request => {
      models.lottery.Lottery.byId(lid).map {
        lottery => retreiveFile(request, lottery.id.get, id)
      } getOrElse NotFound
    }
  }

  def showFile(id: Long, bcode: String) = Action {
    implicit request => {
      models.lottery.Lottery.byBranchCode(bcode.toLong).map {
        lottery => retreiveFile(request, lottery.id.get, id)
      } getOrElse NotFound
    }
  }

  def get(pid: Long) = Authorized(Redakteur) {
    implicit request => {
      Ok(html.lottery.prize.upload_prize(pid, UploadModel.byPrizeId(pid)))
    }
  }

  def count(pid: Long) = Authorized(Redakteur) {
    implicit request => {
      val count = UploadModel.count(pid).getOrElse(0l)
      Ok(toJson(JsObject(List("count" -> JsString(count.toString)))))
    }
  }


  def delete(id: Long) = Authorized(Redakteur) {
    implicit request => {
      val result = UploadModel.deleteById(LotteryId, id).exists(1 ==)
      Ok(JsonUtils.getStatus(result, if (result) "prize.upload.rm.ok" else "prize.upload.rm.notok")).withHeaders(CONTENT_TYPE_JSON)
    }
  }

  def upload(id: Long) = Authorized(Redakteur) {
    implicit request => {
      var back = false
      request.body.asMultipartFormData.headOption.map {
        m => m.file("prize_file").map {
          fp => {
            back = {
              val suffix = fp.filename.substring(fp.filename.lastIndexOf(".") + 1).toLowerCase
              if (validUploadFileTypes.exists(vf => vf.toLowerCase == suffix)) UploadModel.add(LotteryId, id, fp).getOrElse(false)
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
}