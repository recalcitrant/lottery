import models.client.WinningNotificationThread
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Play.current
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

object Global extends GlobalSettings {

	override def onStart(app: Application) {
		// start SMTP-Mock if we are in DEV mode:
	/*	if (Play.isDev) {
			BasicSMTPServer.start(new StdoutMessageHandlerFactory())
		}*/
		WinningNotificationThread.start()
	}

	override def onStop(app: Application) {
		// shutdown SMTP-Mock if we are in DEV mode:
//		if (Play.isDev) BasicSMTPServer.stop()
	}

	override def onError(request: RequestHeader, ex: Throwable) = {
		Future(InternalServerError(
			views.html.error(ex)
		))
	}

	override def onHandlerNotFound(request: RequestHeader) = {
		Future(NotFound(
			views.html.notfound(request.path)
		))
	}

	override def onBadRequest(request: RequestHeader, error: String) = {
		Future(InternalServerError(
			views.html.error(new RuntimeException(error))
		))
	}
}