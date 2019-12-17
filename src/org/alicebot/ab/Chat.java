package org.alicebot.ab;

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
	Library General License for more details.

	You should have received a copy of the GNU Library General Public
	License along with this library; if not, write to the
	Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
	Boston, MA  02110-1301, USA.
*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.alicebot.ab.utils.JapaneseUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Class encapsulating a chat session between a bot and a client
 */
@Slf4j
class Chat {

	Bot bot;
//	boolean doWrites;
	String customerId = Properties.default_Customer_id;
	History<History<?>> thatHistory = new History<History<?>>("that");
	History<String> requestHistory = new History<String>("request");
	History<String> responseHistory = new History<String>("response");
	// History<String> repetitionHistory = new History<String>("repetition");
	History<String> inputHistory = new History<String>("input");
	Predicates predicates = new Predicates();
	static String matchTrace = "";
	static boolean locationKnown = false;
	static String longitude;
	static String latitude;
	TripleStore tripleStore = new TripleStore("anon", bot);

	/**
	 * Constructor
	 * 
	 * @param bot        bot to chat with
	 * @param doWrites   doWrites
	 * @param customerId unique customer identifier
	 */
	// Chat(Bot bot, boolean doWrites, String customerId) {
	Chat(Bot bot, String customerId) {
		this.customerId = customerId;
		this.bot = bot;
		final History<String> contextThatHistory = new History<String>();
		contextThatHistory.add(Properties.default_that);
		thatHistory.add(contextThatHistory);
		final File file = new File(bot.config_path + "/predicates.txt");
		if (file.exists()) {
			try (BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
				String stringLine;
				while ((stringLine = buffer.readLine()) != null) {
					if (stringLine.contains(":")) {
						final String property = stringLine.substring(0, stringLine.indexOf(":"));
						final String value = stringLine.substring(stringLine.indexOf(":") + 1);
						predicates.put(property, value);
					}
				}
			} catch (final Exception ex) {
				log.error(ex.getMessage(), ex);
			}
		}
		addTriples();
		predicates.put("topic", Properties.default_topic);
		predicates.put("jsenabled", Properties.js_enabled);
		log.debug("Chat Session Created for bot " + bot.botName);
	}

	/**
	 * Load Triple Store knowledge base
	 */

	private int addTriples() {
		final File f = new File(bot.config_path + "/triples.txt");
		if (!f.exists()) {
			return 0;
		}
		int tripleCnt = 0;
		log.debug("Loading Triples from " + f.getAbsolutePath());
		try {
			final InputStream is = new FileInputStream(f);
			final BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				final String[] triple = strLine.split(":");
				if (triple.length >= 3) {
					final String subject = triple[0];
					final String predicate = triple[1];
					final String object = triple[2];
					tripleStore.addTriple(subject, predicate, object);
					// Log.i(TAG, "Added Triple:" + subject + " " + predicate + " " + object);
					tripleCnt++;
				}
			}
			is.close();
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		log.debug("Loaded " + tripleCnt + " triples");
		return tripleCnt;
	}

	/**
	 * Return bot response to a single sentence input given conversation context
	 *
	 * @param input              client input
	 * @param that               bot's last sentence
	 * @param topic              current topic
	 * @param contextThatHistory history of "that" values for this request/response
	 *                           interaction
	 * @return bot's reply
	 */
	private String respond(String input, String that, String topic, History<String> contextThatHistory) {
		// Properties.trace("chat.respond(input: " + input + ", that: " + that + ",
		// topic: " + topic + ", contextThatHistory: " + contextThatHistory + ")");
		boolean repetition = true;
		// inputHistory.printHistory();
		for (int i = 0; i < Properties.repetition_count; i++) {
			// log.info(request.toUpperCase()+"=="+inputHistory.get(i)+"?
			// "+request.toUpperCase().equals(inputHistory.get(i)));
			if (inputHistory.get(i) == null || !input.toUpperCase().equals(inputHistory.get(i).toUpperCase())) {
				repetition = false;
			}
		}
		if (input.equals(Properties.null_input)) {
			repetition = false;
		}
		inputHistory.add(input);
		if (repetition) {
			input = Properties.repetition_detected;
		}

		String response;

		response = AIMLProcessor.respond(input, that, topic, this);
		// Properties.trace("in chat.respond(), response: " + response);
		String normResponse = bot.preProcessor.normalize(response);
		// Properties.trace("in chat.respond(), normResponse: " + normResponse);

		normResponse = JapaneseUtils.tokenizeSentence(normResponse);
		final String sentences[] = bot.preProcessor.sentenceSplit(normResponse);
		for (final String sentence : sentences) {
			that = sentence;
			// log.info("That "+i+" '"+that+"'");
			if (that.trim().equals("")) {
				that = Properties.default_that;
			}
			contextThatHistory.add(that);
		}
		final String result = response.trim() + "  ";
		// Properties.trace("in chat.respond(), returning: " + result);
		return result;
	}

	/**
	 * Return bot response given an input and a history of "that" for the current
	 * conversational interaction
	 *
	 * @param input              client input
	 * @param contextThatHistory history of "that" values for this request/response
	 *                           interaction
	 * @return bot's reply
	 */
	String respond(String input, History<String> contextThatHistory) {
		final History<?> hist = thatHistory.get(0);
		String that;
		if (hist == null) {
			that = Properties.default_that;
		} else {
			that = hist.getString(0);
		}
		return respond(input, that, predicates.get("topic"), contextThatHistory);
	}

	final static String NL = System.getProperty("line.separator");

	/**
	 * return a compound response to a multiple-sentence request. "Multiple" means
	 * one or more.
	 *
	 * @param request client's multiple-sentence input
	 * @return string
	 */
	String multisentenceRespond(String request) {

		// Properties.trace("chat.multisentenceRespond(request: " + request + ")");
		String response = "";
		matchTrace = "";
		try {
			String normalized = bot.preProcessor.normalize(request);
			normalized = JapaneseUtils.tokenizeSentence(normalized);
			// Properties.trace("in chat.multisentenceRespond(), normalized: " +
			// normalized);
			final String sentences[] = bot.preProcessor.sentenceSplit(normalized);
			final History<String> contextThatHistory = new History<String>("contextThat");
			for (final String sentence : sentences) {
				// log.info("Human: "+sentences[i]);
				AIMLProcessor.trace_count = 0;
				final String reply = respond(sentence, contextThatHistory);
				response += "  " + reply;
				// log.info("Robot: "+reply);
			}
			requestHistory.add(request);
			responseHistory.add(response);
			thatHistory.add(contextThatHistory);
			response = response.replaceAll("[\n]+", NL);
			response = response.trim();
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
			return Properties.error_bot_response;
		}

//		if (doWrites) {
		bot.writeIFCategories();
//		}
		// Properties.trace("in chat.multisentenceRespond(), returning: " +
		// response);
		return response;
	}

	static void setMatchTrace(String newMatchTrace) {
		matchTrace = newMatchTrace;
	}

}
