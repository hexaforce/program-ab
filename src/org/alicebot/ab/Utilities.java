package org.alicebot.ab;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashSet;

import org.alicebot.ab.utils.CalendarUtils;
import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utilities {

	/**
	 * Excel sometimes adds mysterious formatting to CSV files. This function tries
	 * to clean it up.
	 *
	 * @param line line from AIMLIF file
	 * @return reformatted line
	 */
	public static String fixCSV(String line) {
		while (line.endsWith(";")) {
			line = line.substring(0, line.length() - 1);
		}
		if (line.startsWith("\"")) {
			line = line.substring(1, line.length());
		}
		if (line.endsWith("\"")) {
			line = line.substring(0, line.length() - 1);
		}
		line = line.replaceAll("\"\"", "\"");
		return line;
	}

	public static String tagTrim(String xmlExpression, String tagName) {
		final String stag = "<" + tagName + ">";
		final String etag = "</" + tagName + ">";
		if (xmlExpression.length() >= (stag + etag).length()) {
			xmlExpression = xmlExpression.substring(stag.length());
			xmlExpression = xmlExpression.substring(0, xmlExpression.length() - etag.length());
		}
		return xmlExpression;
	}

	public static HashSet<String> stringSet(String... strings) {
		final HashSet<String> set = new HashSet<String>();
		for (final String s : strings) {
			set.add(s);
		}
		return set;
	}

	public static String getCopyright(Bot bot, String AIMLFilename) {
		String copyright = "";
		final String year = CalendarUtils.year();
		final String date = CalendarUtils.date();
		try {
			copyright = FileUtils.readFileToString(new File(bot.config_path + "/copyright.txt"), Charset.defaultCharset());
			copyright = copyright.replace("[url]", bot.properties.get("url"));
			copyright = copyright.replace("[date]", date);
			copyright = copyright.replace("[YYYY]", year);
			copyright = copyright.replace("[version]", bot.properties.get("version"));
			copyright = copyright.replace("[botname]", bot.botName.toUpperCase());
			copyright = copyright.replace("[filename]", AIMLFilename);
			copyright = copyright.replace("[botmaster]", bot.properties.get("botmaster"));
			copyright = copyright.replace("[organization]", bot.properties.get("organization"));
		} catch (final Exception e) {// Catch exception if any
			log.error("Error: " + e.getMessage());
		}
		return copyright;
	}

}
