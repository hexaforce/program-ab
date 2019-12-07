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

//	public static String getFileFromInputStream(InputStream in) {
//		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
//		String strLine;
//		// Read File Line By Line
//		String contents = "";
//		try {
//			while ((strLine = br.readLine()) != null) {
//				if (!strLine.startsWith(MagicStrings.text_comment_mark)) {
//					if (strLine.length() == 0) {
//						contents += "\n";
//					} else {
//						contents += strLine + "\n";
//					}
//				}
//			}
//		} catch (final Exception ex) {
//			log.error(ex.getMessage(), ex);
//		}
//		return contents.trim();
//	}

//	public static String getFile(String filename) {
//		String contents = "";
//		try {
//			final File file = new File(filename);
//			if (file.exists()) {
//				// log.info("Found file "+filename);
//				final FileInputStream fstream = new FileInputStream(filename);
//				// Get the object
//				contents = getFileFromInputStream(fstream);
//				fstream.close();
//			}
//		} catch (final Exception e) {// Catch exception if any
//			log.error("Error: " + e.getMessage());
//		}
//		// log.info("getFile: "+contents);
//		return contents;
//	}

//	public static String getCopyrightFromInputStream(InputStream in) {
//		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
//		String strLine;
//		// Read File Line By Line
//		String copyright = "";
//		try {
//			while ((strLine = br.readLine()) != null) {
//				if (strLine.length() == 0) {
//					copyright += "\n";
//				} else {
//					copyright += "<!-- " + strLine + " -->\n";
//				}
//			}
//		} catch (final Exception ex) {
//			log.error(ex.getMessage(), ex);
//		}
//		return copyright;
//	}

	public static String getCopyright(Bot bot, String AIMLFilename) {
		String copyright = "";
		final String year = CalendarUtils.year();
		final String date = CalendarUtils.date();
		//String NL = System.getProperty("line.separator");
		try {
			copyright = FileUtils.readFileToString(new File(bot.config_path + "/copyright.txt"), Charset.defaultCharset());
//			final String[] splitCopyright = copyright.split("\n");
//			copyright = "";
//			for (final String element : splitCopyright) {
//				copyright += element;
//			}
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
//		copyright += "<!--  -->\n";
		// log.info("Copyright: "+copyright);
		return copyright;
	}

//	public static String getPannousAPIKey(Bot bot) {
//		String apiKey = getFile(bot.config_path + "/pannous-apikey.txt");
//		if (apiKey.equals("")) {
//			apiKey = MagicStrings.pannous_api_key;
//		}
//		return apiKey;
//	}
//
//	public static String getPannousLogin(Bot bot) {
//		String login = getFile(bot.config_path + "/pannous-login.txt");
//		if (login.equals("")) {
//			login = MagicStrings.pannous_login;
//		}
//		return login;
//	}

	/**
	 * Returns if a character is one of Chinese-Japanese-Korean characters.
	 *
	 * @param c the character to be tested
	 * @return true if CJK, false otherwise
	 */
//	public static boolean isCharCJK(final char c) {
//		if ((Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS) || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT) || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS)) {
//			return true;
//		}
//		return false;
//	}

}
