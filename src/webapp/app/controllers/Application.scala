package controllers


import play.api._
import play.api.mvc._


object Application extends Controller {

	def javascriptRoutes = Action {
		implicit request => {
			Ok(Routes.javascriptRouter("jsRoutes")(
				controllers.auth.routes.javascript.Users.get,
				controllers.auth.routes.javascript.Users.addnew,
				controllers.lottery.routes.javascript.Lottery.get
			)).as("text/javascript")
		}
	}
}