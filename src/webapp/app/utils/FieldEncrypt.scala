package utils

import play.api.Play
import play.api.libs.Crypto
import play.api.Play.current

object FieldEncrypt {

	def sign(field: Long) =
		Crypto.sign(field + "", Play.configuration.getString("form.hash.key").get.getBytes)
}