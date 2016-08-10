package models.lottery.drawing

import anorm.NotAssigned

import play.api.libs.json._
import models.lottery.prize.PrizeType
import utils.{Format, DateUtils}

object DrawingJsonFormat {

	implicit object DrawingFormat extends Format[Drawing] {

		def reads(json: JsValue) = JsSuccess(Drawing(NotAssigned, null, Seq[DrawingPrize](), None, None, None, None, None, None, None, None))

		def writes(drawing: Drawing): JsValue = {

			def foldPrizeCount(prizes: Seq[models.lottery.prize.PrizeCount]) = {
				prizes.foldLeft(JsArray()) {
					(acc, pcount) =>
						val amount = pcount.value.map(value => if ("000" != value) Format.currency(pcount.value.getOrElse("0")) else "").getOrElse("")
						val sum = if (0 != pcount.total) Format.currency(pcount.total.toString) else ""
						acc :+ JsObject(List(
							"type" -> JsString(pcount.prizeType.map(_.toString).getOrElse("-1")),
							"count" -> JsString(pcount.times.map(t => Format.formatNumber(t).toString).getOrElse("0")),
							"title" -> JsString(pcount.title.getOrElse("")),
							"amount" -> JsString(amount),
							"sum" -> JsString(sum),
							"id" -> JsString(pcount.pid.toString)))
				}
			}

			def foldDigits(prizes: Seq[models.lottery.drawing.DrawingPrize]) = {
				prizes.foldLeft(JsArray()) {
					(acc, prize) => acc :+ JsObject(List(
						"type" -> JsString(prize.prize.map(_.prizeType.id.map(_.toString).getOrElse("-1")).get),
						"digits" -> JsString(prize.finalDigits.map(_.digits).get),
						"prize" -> JsString(prize.prize.map(_.display).get),
						"id" -> JsString(prize.prize.map(_.id.map(_.toString).getOrElse("-1")).get)))
				}
			}

			val datenext = JsString(drawing.dateNext.map(dt => "Die nächste Ziehung findet am " + DateUtils.date2Str(dt) + " statt.").getOrElse(""))
			val datepublishnext = JsString(drawing.datePublishNext.map(dt => "Die nächste Veröffentlichung findet am " + DateUtils.date2Str(dt) + " statt.").getOrElse(""))

			JsObject(List(
				"id" -> JsString(drawing.id.map(_.toString).getOrElse("-1")),
				"type" -> JsString(drawing.dbase.drawingType.id.map(_.toString).getOrElse("-1")),
				"date" -> JsString(drawing.date.map(DateUtils.date2Str(_)).getOrElse("")),
				"datenext" -> datenext,
				"datepublishnext" -> datepublishnext,
				"finaldigits" -> foldDigits(drawing.prizes.filter(dp => dp.finalDigits.isDefined)),
				"prizecount" -> foldPrizeCount(drawing.prizeCount.map(_.filter(_.times.isDefined)).getOrElse(Nil)),
				"show_prizecount" -> JsString(drawing.prizeCount.map(_.exists(_.times.getOrElse(0l) != 0l)).map(exists => if (exists) "1" else "0").get),
				"totalcount" ->
					JsObject(List(
						"count" -> JsString(Format.formatNumber(drawing.totalCount).toString),
						"amount" -> JsString(Format.frontendCurrency(drawing.totalAmount))))))
		}
	}

	implicit object DrawingResultFormat extends Format[DrawingResult] {

		def reads(json: JsValue) = JsSuccess(DrawingResult(-1, "", 1, None, -1, TicketDigit(NotAssigned, "", None), ""))

		def writes(drawing: DrawingResult): JsValue = {
			JsObject(List(
				"finaldigits" -> JsString(drawing.digits.digits),
				"type" -> JsString(drawing.pType.toString),
				"prize" -> JsString(if (PrizeType.CASH == drawing.pType) Format.currency(drawing.amount) else drawing.pTitle.map(t => t).getOrElse("")),
				"id" -> JsString(drawing.pid.toString)))
		}
	}

}
