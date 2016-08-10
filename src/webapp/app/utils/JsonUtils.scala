package utils

import play.api.data.FormError
import play.api.i18n.Lang
import play.api.libs.json._
import play.api.libs.json.Json._

object JsonUtils {

	def getErrors(errors: Seq[FormError], msg: String = "Bitte beachten Sie die rot markierten Felder") =
		stringify(obj("status" -> "!ok",
			"msg" -> play.api.i18n.Messages(msg)(Lang("de")),
			"errors" ->
				errors.foldLeft(JsArray()) {
					(acc, item) => {
						acc :+ Json.obj("key" -> item.key, "message" -> play.api.i18n.Messages(item.message, 5)(Lang("de")))
					}
				}))

	def getStatus(status: Boolean, msg: String = "none") = stringify(toJson(obj("status" -> (if (status) "ok" else "!ok"), "msg" -> play.api.i18n.Messages.apply(msg))))

	def map(vals: Map[String, String]) = stringify(toJson(vals))
}