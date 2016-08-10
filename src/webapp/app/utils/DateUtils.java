package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

	public static Date str2Date(String strdate) {
		try {
			return new SimpleDateFormat("dd.MM.yyyy").parse(strdate);
		} catch (ParseException e) {
			return new Date();
		}
	}

	public static String date2Str(Date date) {
		try {
			return new SimpleDateFormat("dd.MM.yyyy").format(date);
		} catch (Exception e) {
			return "";
		}
	}

	public static String date2TicketStr(Date date) {
			try {
				return new SimpleDateFormat("ddMMyyyy").format(date);
			} catch (Exception e) {
				return "";
			}
		}

	public static Date str2DateHourMinutes(String strdate) {
		try {
			return new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(strdate);
		} catch (ParseException e) {
			return new Date();
		}
	}

	public static String dateTime2Str(Date date) {
		try {
			return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(date);
		} catch (Exception e) {
			return "";
		}
	}
}
