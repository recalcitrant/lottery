package controllers.lottery

import play.api.mvc._
import views.html
import controllers.auth.{UserNav, Auth}
import play.api.libs.iteratee.Enumerator
import utils.Matcher
import models.lottery.{Lottery => LotteryModel}

object Lottery extends Controller with Auth with UserNav {
 
    def privacyTerms(bcode: Long) = Action {
    implicit request => {
      LotteryModel.privacyTerms(bcode).map {
        case (file) =>
	  import play.api.libs.concurrent.Execution.Implicits._
          val fileContent: Enumerator[Array[Byte]] = Enumerator.fromFile(file)
          val agent = request.headers.get(USER_AGENT).getOrElse("empty")
          // if IE <= 8:
          val header = if (Matcher.is_IE_up_2_version_8(agent)) "attachment; filename=datenschutz.pdf"
          // all other browsers (including IE >= 9
          else "attachment; filename*=UTF-8''datenschutz.pdf"
          SimpleResult(
            header = ResponseHeader(200, Map(
              CONTENT_DISPOSITION -> header,
              CONTENT_LENGTH -> file.length.toString,
              CONTENT_TYPE -> CONTENT_TYPE_BINARY)),
            body = fileContent
          )
      } getOrElse NotFound
    }
  }

  def list = {
    Authorized(SuperAdmin) {
      implicit request =>
        Ok(html.lottery.list(LotteryModel.list, getNav(Some(user))))
    }
  }

  def get(id: Long) = {
    Authorized(SuperAdmin) {
      implicit request =>
        Ok(html.lottery.get(LotteryModel.byId(id).get, getNav(Some(user))))
    }
  }
}
