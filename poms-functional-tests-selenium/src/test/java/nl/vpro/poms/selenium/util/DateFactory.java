package nl.vpro.poms.selenium.util;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateFactory {
	private static final Format SDF =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private static final String TODAY_DATE_FORMAT = "dd-MM-yyyy";
	private static final Format TODAY_FORMAT =
			new SimpleDateFormat(TODAY_DATE_FORMAT);


	public static String getNow() {
		Date now = Calendar.getInstance().getTime();
		return SDF.format(now);
	}
	
	public static String getToday() {
		Date today = Calendar.getInstance().getTime();
		return TODAY_FORMAT.format(today);
	}

	public static String getPastDay() {
		final Instant instantDay = Instant.now().minus(Duration.ofDays(2));
		Date dayAgo = Date.from(instantDay);
		return TODAY_FORMAT.format(dayAgo);
	}
}
