package utils

object NumberUtils {

  def str2Long(str: String) = {
    try {
      str.toLong
    } catch {
      case ex: NumberFormatException => 0l
    }
  }
}