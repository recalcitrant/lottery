package utils

object Matcher {

  // IE 9 and above should NOT match:
  private val ie8Matcher = """(?i).*MSIE [5-8]\..*""".r.pattern
  // IE 9 SHOULD match:
  private val ieMatcher = """(?i).*MSIE [5-9]\..*""".r.pattern

  def is_IE_up_2_version_8(str: String) = ie8Matcher.matcher(str).matches()
  def isIE(str: String) = ieMatcher.matcher(str).matches()
}