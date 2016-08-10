package utils

import java.util.{Date, Locale}
import java.text.{DecimalFormatSymbols, DecimalFormat, DateFormat, NumberFormat}

object Format {

	def currency(value: String) = {
		var back = ""
		if ("" != value.trim() && "0" != value.trim()) {
			try {
				back = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(java.lang.Double.parseDouble(value.substring(0, value.length() - 2) + "." + value.substring(value.length() - 2)))
			} catch {
				case ex: Throwable => back = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(0.0)
			}
		} else back = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(0.0)
		back.replace("€", "Euro")
	}

	def frontendCurrency(value: String) = {
		var back = ""
		if ("" != value.trim() && "0" != value.trim()) {
			try {
				back = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(java.lang.Double.parseDouble(value.substring(0, value.length() - 2) + "." + value.substring(value.length() - 2)))
			} catch {
				case ex: Throwable => back = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(0.0)
			}
		} else back = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(0.0)
		back.replace(" €", "&nbsp;Euro")
	}

	def amountWithComma(value: String) = {
		var back = ""
		if ("" != value.trim() && "0" != value.trim()) {
			try {
				back = value.substring(0, value.length() - 2) + "," + value.substring(value.length() - 2)
			} catch {
				case ex: Throwable => back = ""
			}
		} else back = ""
		back
	}

	def currencyInput(value: String) = value.substring(0, value.length() - 2) + "," + value.substring(value.length() - 2)

	def date(value: Date) = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMANY).format(value)

	def dateWithTime(value: Date) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.GERMANY).format(value)

	def formatNumber(num: Long) = {
		val geSymbols = new DecimalFormatSymbols(Locale.GERMAN)
		val decimalFormat = new DecimalFormat("#,##0")
		decimalFormat.setDecimalFormatSymbols(geSymbols)
		decimalFormat.format(num)
	}
}