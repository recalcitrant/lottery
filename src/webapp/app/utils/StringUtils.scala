package utils

object StringUtils {

  val numPattern = """^[0-9]{1,9}$""".r.pattern

  def numbersOnly(str: String) = numPattern.matcher(str).matches()

  def removeIdPrefix(str: String) = str.substring(str.indexOf("_") + 1)

  def encode4Download(encode: => (String, String) => String, filename: String) = {
    val now = System.currentTimeMillis()
    val SPACE = now + "_space"
    val PLUS = now + "_plus"
    val PERCENT = now + "_percent"
    val fname = filename.replace(" ", SPACE).replace("+", PLUS).replace("%", PERCENT)
    val medname = encode(fname, "UTF-8")
    medname.replace(SPACE, "%20").replace(PLUS, "%2b").replace(PERCENT, "%25")
  }

  def trim(str: String) = if (null != str) str.trim else ""
}