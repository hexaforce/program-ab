package org.alicebot.ab.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CalendarUtils {

	public static int timeZoneOffset() {
		Calendar cal = Calendar.getInstance();
		int offset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000);
		return offset;
	}

	public static String year() {
		Calendar cal = Calendar.getInstance();
		return String.valueOf(cal.get(Calendar.YEAR));
	}

	public static String date() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMMMMMM dd, yyyy");
		dateFormat.setCalendar(cal);
		return dateFormat.format(cal.getTime());
	}

	public static String date(String jformat, String locale, String timezone) {
		if (jformat == null)
			jformat = "EEE MMM dd HH:mm:ss zzz yyyy";
		if (locale == null)
			locale = Locale.US.getISO3Country();
		if (timezone == null)
			timezone = TimeZone.getDefault().getDisplayName();
		String dateAsString = new Date().toString();
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(jformat);
			dateAsString = simpleDateFormat.format(new Date());
		} catch (Exception ex) {
			log.info("CalendarUtils.date Bad date: Format = " + jformat + " Locale = " + locale + " Timezone = " + timezone);
			log.error(ex.getMessage(), ex);
		}
		return dateAsString;
	}

}
