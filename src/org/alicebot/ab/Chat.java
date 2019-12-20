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
import java.io.InputStreamReader;

import lombok.extern.slf4j.Slf4j;

/**
 * Class encapsulating a chat session between a bot and a client
 */
@Slf4j
class Chat {

	Bot bot;
	String customerId = Properties.default_Customer_id;
	History<History<?>> thatHistory = new History<History<?>>("that");
	History<String> requestHistory = new History<String>("request");
	History<String> responseHistory = new History<String>("response");
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
		
		final File predicatesFile = new File(bot.config_path + "/predicates.txt");
		if (predicatesFile.exists()) {
			try (BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream(predicatesFile)))) {
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

		final File triplesFile = new File(bot.config_path + "/triples.txt");
		if (triplesFile.exists()) {
			try (BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream(triplesFile)))) {
				String stringLine;
				while ((stringLine = buffer.readLine()) != null) {
					final String[] triple = stringLine.split(":");
					if (triple.length >= 3) {
						final String subject = triple[0];
						final String predicate = triple[1];
						final String object = triple[2];
						tripleStore.addTriple(subject, predicate, object);
					}
				}
			} catch (final Exception ex) {
				log.error(ex.getMessage(), ex);
			}
		}
		
		predicates.put("topic", Properties.default_topic);
		predicates.put("jsenabled", Properties.js_enabled);
		log.debug("Chat Session Created for bot " + bot.botName);
	}

	/**
	 * @param newMatchTrace
	 */
	static void setMatchTrace(String newMatchTrace) {
		matchTrace = newMatchTrace;
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
		
		boolean repetition = true;
		for (int i = 0; i < Properties.repetition_count; i++) {
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

		String response = AIMLProcessor.respond(input, that, topic, this, 0);
		
		for (final String sentence : bot.preProcessor.japaneseSentenceSplit(response)) {
			that = sentence;
			if (that.trim().equals("")) {
				that = Properties.default_that;
			}
			contextThatHistory.add(that);
		}
		return response.trim() + "  ";
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
		
		String response = "";
		matchTrace = "";
		
		try {
			
			final History<String> contextThatHistory = new History<String>("contextThat");
			
			for (final String sentence : bot.preProcessor.japaneseSentenceSplit(request)) {
				AIMLProcessor.trace_count = 0;
				
				final History<?> hist = thatHistory.get(0);
				String that;
				if (hist == null) {
					that = Properties.default_that;
				} else {
					that = hist.getString(0);
				}
				
				final String reply = respond(sentence, that, predicates.get("topic"), contextThatHistory);
				
				response += "  " + reply;
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
		
		bot.writeIFCategories();
		return response;
	}

}
