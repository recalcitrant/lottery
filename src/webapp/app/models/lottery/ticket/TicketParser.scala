package models.lottery.ticket

import scala.io.Source
import java.io.File
import collection.mutable.ListBuffer
import java.util.Date
import exception.{ExceptionCodes, AppException}
import models.lottery.drawing.Drawing

object TicketParser {

  def parse(file: File, drawing: Drawing) = {
    val ret = ListBuffer[(String, String)]()
    val stripLeadingZeros = "\\b0*([1-9][0-9]*|0)\\b".r
    try {
      Source.fromFile(file).getLines().foreach {
        line => {
          val trimmed = line.trim
          if (30 == trimmed.length()) {
            val preticket = line.substring(0, 9)
            val ticket = stripLeadingZeros replaceAllIn(preticket, "$1")
            val preamount = line.substring(9, 16) + "00"
            val amount = stripLeadingZeros replaceAllIn(preamount, "$1")
            val checkdate = line.substring(16, 24)
            val ddate = utils.DateUtils.date2TicketStr(drawing.date.getOrElse(new Date))
            if (checkdate != ddate) throw AppException(ExceptionCodes.TICKET_PARSING_WRONG_DATE)
            val t = (ticket, amount)
            ret += t
          } else throw AppException(ExceptionCodes.TICKET_PARSING_CORRUPT_UPLOAD)
        }
      }
      Right(ret)
    } catch {
      case ex: Throwable => Left(ex)
    }
  }
}