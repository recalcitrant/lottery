package test

case class MessageValidator(expected: String, totest: String) {

	def validate(actual: String) = {
		expected == actual
	}
}

object MessageValidator {
	val TEST_SUBJECT = "testsubject"
	val TEST_BODY = "testbody"
}

