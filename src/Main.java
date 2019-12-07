
/* 
	Program AB Reference AIML 2.1 implementation

	Copyright (C) 2013 ALICE A.I. Foundation
	Contact: info@alicebot.org

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Library General Public
	License as published by the Free Software Foundation; either
	version 2 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Library General Public License for more details.

	You should have received a copy of the GNU Library General Public
	License along with this library; if not, write to the
	Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
	Boston, MA  02110-1301, USA.
*/

import java.io.IOException;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicNumbers;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;
import org.alicebot.ab.utils.IOUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

//	public static void main(String[] args) {
////		MagicStrings.setRootPath();
//		AIMLProcessor.extension = new PCAIMLProcessorExtension();
//		mainFunction(args);
//	}

	public static void main(String[] args) throws IOException {
		AIMLProcessor.extension = new PCAIMLProcessorExtension();

		//String botName = "alise2";
		String botName = "アリス";
		String workingDirectory = System.getProperty("user.dir");
//		String action = "chat";
		log.info(MagicStrings.program_name_version);
		for (final String s : args) {
			// log.info(s);
			final String[] splitArg = s.split("=");
			if (splitArg.length >= 2) {
				final String option = splitArg[0];
				final String value = splitArg[1];
				// if (MagicBooleans.trace_mode) log.info(option+"='"+value+"'");
				if (option.equals("bot")) {
					botName = value;
				}
//				if (option.equals("action")) {
//					action = value;
//				}
			}
		}
		log.debug("Working Directory = " + workingDirectory);
//		Graphmaster.ENABLE_SHORT_CUTS = true;
		// Timer timer = new Timer();

		//final Bot bot = new Bot(botName, path, action); //
		final Bot bot = new Bot(workingDirectory, botName); //

		// EnglishNumberToWords.makeSetMap(bot);
		// getGloss(bot, "c:/ab/data/wn30-lfs/wne-2006-12-06.xml");
//		if (MagicBooleans.make_verbs_sets_maps) {
//			Verbs.makeVerbSetsMaps(bot);
//		}
		// bot.preProcessor.normalizeFile("c:/ab/data/log2.txt",
		// "c:/ab/data/log2normal.txt");
		// System.exit(0);
		if (bot.brain.getCategories().size() < MagicNumbers.brain_print_size) {
			bot.brain.printgraph();
		}
		
		final Chat chatSession = new Chat(bot, "0");

		bot.brain.nodeStats();
		String textLine = "";
		while (true) {
			textLine = IOUtils.readInputTextLine("Human");
			if (textLine == null || textLine.length() < 1) {
				textLine = MagicStrings.null_input;
			}
			if (textLine.equals("q")) {
				System.exit(0);
			} else if (textLine.equals("wq")) {
				bot.writeQuit();
				System.exit(0);
			} else {
				final String request = textLine;
				log.debug("STATE=" + request + ":THAT=" + chatSession.thatHistory.get(0).get(0) + ":TOPIC=" + chatSession.predicates.get("topic"));
				String response = chatSession.multisentenceRespond(request);
				while (response.contains("&lt;")) {
					response = response.replace("&lt;", "<");
				}
				while (response.contains("&gt;")) {
					response = response.replace("&gt;", ">");
				}
				IOUtils.writeOutputTextLine("Robot", response);
			}
		}
//		log.debug("Action = '" + action + "'");

//		if (action.equals("chat") || action.equals("chat-app")) {
//		final boolean doWrites = !action.equals("chat-app");
//		testChat(bot);
//		}
//		// else if (action.equals("test")) testSuite(bot,
//		// MagicStrings.root_path+"/data/find.txt");
//		else if (action.equals("ab"))
//			TestAB.testAB(bot, TestAB.sample_file);
//		else if (action.equals("aiml2csv") || action.equals("csv2aiml"))
//			convert(bot, action);
//		else if (action.equals("abwq")) {
//			AB ab = new AB(bot, TestAB.sample_file);
//			ab.abwq();
//		} else if (action.equals("test")) {
//			TestAB.runTests(bot);
//		} else if (action.equals("shadow")) {
//			bot.shadowChecker();
//		} else if (action.equals("iqtest")) {
//			ChatTest ct = new ChatTest(bot);
//			try {
//				ct.testMultisentenceRespond();
//			} catch (Exception ex) {
//				log.error(ex.getMessage(), ex);
//			}
//		} else
//			log.info("Unrecognized action " + action);
	}

//	public static void convert(Bot bot, String action) {
//		if (action.equals("aiml2csv")) {
//			bot.writeAIMLIFFiles();
//		} else if (action.equals("csv2aiml")) {
//			bot.writeAIMLFiles();
//		}
//	}

//	public static void getGloss(Bot bot, String filename) {
//		log.info("getGloss");
//		try {
//			// Open the file that is the first
//			// command line parameter
//			final File file = new File(filename);
//			if (file.exists()) {
//				final FileInputStream fstream = new FileInputStream(filename);
//				// Get the object
//				getGlossFromInputStream(bot, fstream);
//				fstream.close();
//			}
//		} catch (final Exception e) {// Catch exception if any
//			log.error("Error: " + e.getMessage());
//		}
//	}
//
//	public static void getGlossFromInputStream(Bot bot, InputStream in) {
//		log.info("getGlossFromInputStream");
//		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
//		String strLine;
//		int cnt = 0;
//		int filecnt = 0;
//		final HashMap<String, String> def = new HashMap<String, String>();
//		try {
//			// Read File Line By Line
//			String word;
//			String gloss;
//			word = null;
//			gloss = null;
//			while ((strLine = br.readLine()) != null) {
//
//				if (strLine.contains("<entry word")) {
//					final int start = strLine.indexOf("<entry word=\"") + "<entry word=\"".length();
//					// int end = strLine.indexOf(" status=");
//					final int end = strLine.indexOf("#");
//					word = strLine.substring(start, end);
//					word = word.replaceAll("_", " ");
//					log.info(word);
//				} else if (strLine.contains("<gloss>")) {
//					gloss = strLine.replaceAll("<gloss>", "");
//					gloss = gloss.replaceAll("</gloss>", "");
//					gloss = gloss.trim();
//					log.info(gloss);
//				}
//
//				if (word != null && gloss != null) {
//					word = word.toLowerCase().trim();
//					if (gloss.length() > 2) {
//						gloss = gloss.substring(0, 1).toUpperCase() + gloss.substring(1, gloss.length());
//					}
//					String definition;
//					if (def.keySet().contains(word)) {
//						definition = def.get(word);
//						definition = definition + "; " + gloss;
//					} else {
//						definition = gloss;
//					}
//					def.put(word, definition);
//					word = null;
//					gloss = null;
//				}
//			}
//			final Category d = new Category(0, "WNDEF *", "*", "*", "unknown", "wndefs" + filecnt + ".aiml");
//			bot.brain.addCategory(d);
//			for (final String x : def.keySet()) {
//				word = x;
//				gloss = def.get(word) + ".";
//				cnt++;
//				if (cnt % 5000 == 0) {
//					filecnt++;
//				}
//
//				final Category c = new Category(0, "WNDEF " + word, "*", "*", gloss, "wndefs" + filecnt + ".aiml");
//				log.info(cnt + " " + filecnt + " " + c.inputThatTopic() + ":" + c.getTemplate() + ":" + c.getFilename());
//				Nodemapper node;
//				if ((node = bot.brain.findNode(c)) != null) {
//					node.category.setTemplate(node.category.getTemplate() + "," + gloss);
//				}
//				bot.brain.addCategory(c);
//			}
//		} catch (final Exception ex) {
//			log.error(ex.getMessage(), ex);
//		}
//	}
//
//	public static void sraixCache(String filename, Chat chatSession) {
//		final int limit = 1000;
//		try {
//			final FileInputStream fstream = new FileInputStream(filename);
//			// Get the object
//			final BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
//			String strLine;
//			// Read File Line By Line
//			int count = 0;
//			while ((strLine = br.readLine()) != null && count++ < limit) {
//				log.info("Human: " + strLine);
//				final String response = chatSession.multisentenceRespond(strLine);
//				log.info("Robot: " + response);
//			}
//			br.close();
//		} catch (final Exception ex) {
//			log.error(ex.getMessage(), ex);
//		}
//	}

}
