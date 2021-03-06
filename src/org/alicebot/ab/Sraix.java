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
	Library General Public License for more details.

	You should have received a copy of the GNU Library General Public
	License along with this library; if not, write to the
	Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
	Boston, MA  02110-1301, USA.
*/

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alicebot.ab.utils.BotProperties;
import org.alicebot.ab.utils.CalendarUtils;
import org.alicebot.ab.utils.NetworkUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Sraix {

	private static HashMap<String, String> custIdMap = new HashMap<String, String>();

	private static String custid = "1"; // customer ID number for Pandorabots

	static String sraix(Chat chatSession, String input, String defaultResponse, String hint, String host, String botid, String apiKey, String limit) {
		String response;
		if (!BotProperties.enable_network_connection) {
			response = BotProperties.sraix_failed;
		} else if (host != null && botid != null) {
			response = sraixPandorabots(input, chatSession, host, botid);
		} else {
			response = sraixPannous(input, hint, chatSession);
		}
		log.info("Sraix: response = " + response + " defaultResponse = " + defaultResponse);
		if (response.equals(BotProperties.sraix_failed)) {
			if (chatSession != null && defaultResponse == null) {
				response = AIMLProcessor.respond(BotProperties.sraix_failed, "nothing", "nothing", chatSession);
			} else if (defaultResponse != null) {
				response = defaultResponse;
			}
		}
		return response;
	}

	private static String sraixPandorabots(String input, Chat chatSession, String host, String botid) {
		// log.info("Entering SRAIX with input="+input+" host ="+host+"
		// botid="+botid);
		final String responseContent = pandorabotsRequest(input, host, botid);
		if (responseContent == null) {
			return BotProperties.sraix_failed;
		} else {
			return pandorabotsResponse(responseContent, chatSession, host, botid);
		}
	}

	private static String pandorabotsRequest(String input, String host, String botid) {
		try {
			custid = "0";
			final String key = host + ":" + botid;
			if (custIdMap.containsKey(key)) {
				custid = custIdMap.get(key);
			}
			final String spec = NetworkUtils.spec(host, botid, custid, input);

			log.debug("Spec = " + spec);

			final String responseContent = NetworkUtils.responseContent(spec);
			// log.info("Sraix: Response="+responseContent);
			return responseContent;
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
			return null;
		}
	}

	private static String pandorabotsResponse(String sraixResponse, Chat chatSession, String host, String botid) {
		String botResponse = BotProperties.sraix_failed;
		try {
			int n1 = sraixResponse.indexOf("<that>");
			int n2 = sraixResponse.indexOf("</that>");

			if (n2 > n1) {
				botResponse = sraixResponse.substring(n1 + "<that>".length(), n2);
			}
			n1 = sraixResponse.indexOf("custid=");
			if (n1 > 0) {
				custid = sraixResponse.substring(n1 + "custid=\"".length(), sraixResponse.length());
				n2 = custid.indexOf("\"");
				if (n2 > 0) {
					custid = custid.substring(0, n2);
				} else {
					custid = "0";
				}
				final String key = host + ":" + botid;
				// log.info("--> Map "+key+" --> "+custid);
				custIdMap.put(key, custid);
			}
			if (botResponse.endsWith(".")) {
				botResponse = botResponse.substring(0, botResponse.length() - 1); // snnoying Pandorabots extra "."
			}
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return botResponse;
	}

	private static String sraixPannous(String input, String hint, Chat chatSession) {
		try {
			final String rawInput = input;
			if (hint == null) {
				hint = BotProperties.sraix_no_hint;
			}
			input = " " + input + " ";
			input = input.replace(" point ", ".");
			input = input.replace(" rparen ", ")");
			input = input.replace(" lparen ", "(");
			input = input.replace(" slash ", "/");
			input = input.replace(" star ", "*");
			input = input.replace(" dash ", "-");
			// input = chatSession.bot.preProcessor.denormalize(input);
			input = input.trim();
			input = input.replace(" ", "+");
			final int offset = CalendarUtils.timeZoneOffset();
			// log.info("OFFSET = "+offset);
			String locationString = "";
			if (Chat.locationKnown) {
				locationString = "&location=" + Chat.latitude + "," + Chat.longitude;
			}
			// https://weannie.pannous.com/api?input=when+is+daylight+savings+time+in+the+us&locale=en_US&login=pandorabots&ip=169.254.178.212&botid=0&key=CKNgaaVLvNcLhDupiJ1R8vtPzHzWc8mhIQDFSYWj&exclude=Dialogues,ChatBot&out=json
			// exclude=Dialogues,ChatBot&out=json&clientFeatures=show-images,reminder,say&debug=true
			final String url = "https://ask.pannous.com/api?input=" + input + "&locale=en_US&timeZone=" + offset + locationString + "&login=" + "test-user" + "&ip=" + NetworkUtils.localIPAddress() + "&botid=0&key=" + "guest" + "&exclude=Dialogues,ChatBot&out=json&clientFeatures=show-images,reminder,say&debug=true";
			log.trace("in Sraix.sraixPannous, url: '" + url + "'");
			final String page = NetworkUtils.responseContent(url);
			// MagicBooleans.trace("in Sraix.sraixPannous, page: " + page);
			String text = "";
			String imgRef = "";
			String urlRef = "";
			if (page == null || page.length() == 0) {
				text = BotProperties.sraix_failed;
			} else {
				final JSONArray outputJson = new JSONObject(page).getJSONArray("output");
				// MagicBooleans.trace("in Sraix.sraixPannous, outputJson class: " +
				// outputJson.getClass() + ", outputJson: " + outputJson);
				if (outputJson.length() == 0) {
					text = BotProperties.sraix_failed;
				} else {
					final JSONObject firstHandler = outputJson.getJSONObject(0);
					// MagicBooleans.trace("in Sraix.sraixPannous, firstHandler class: " +
					// firstHandler.getClass() + ", firstHandler: " + firstHandler);
					final JSONObject actions = firstHandler.getJSONObject("actions");
					// MagicBooleans.trace("in Sraix.sraixPannous, actions class: " +
					// actions.getClass() + ", actions: " + actions);
					if (actions.has("reminder")) {
						// MagicBooleans.trace("in Sraix.sraixPannous, found reminder action");
						final Object obj = actions.get("reminder");
						if (obj instanceof JSONObject) {
							log.debug("Found JSON Object");
							final JSONObject sObj = (JSONObject) obj;
							String date = sObj.getString("date");
							date = date.substring(0, "2012-10-24T14:32".length());
							log.debug("date=" + date);
							final String duration = sObj.getString("duration");
							log.debug("duration=" + duration);

							final Pattern datePattern = Pattern.compile("(.*)-(.*)-(.*)T(.*):(.*)");
							final Matcher m = datePattern.matcher(date);
							String year = "", month = "", day = "", hour = "", minute = "";
							if (m.matches()) {
								year = m.group(1);
								month = String.valueOf(Integer.parseInt(m.group(2)) - 1);
								day = m.group(3);

								hour = m.group(4);
								minute = m.group(5);
								text = "<year>" + year + "</year>" + "<month>" + month + "</month>" + "<day>" + day + "</day>" + "<hour>" + hour + "</hour>" + "<minute>" + minute + "</minute>" + "<duration>" + duration + "</duration>";

							} else {
								text = BotProperties.schedule_error;
							}
						}
					} else if (actions.has("say") && !hint.equals(BotProperties.sraix_pic_hint) && !hint.equals(BotProperties.sraix_shopping_hint)) {
						log.trace("in Sraix.sraixPannous, found say action");
						final Object obj = actions.get("say");
						if (obj instanceof JSONObject) {
							final JSONObject sObj = (JSONObject) obj;
							text = sObj.getString("text");
							if (sObj.has("moreText")) {
								final JSONArray arr = sObj.getJSONArray("moreText");
								for (int i = 0; i < arr.length(); i++) {
									text += " " + arr.getString(i);
								}
							}
						} else {
							text = obj.toString();
						}
					}
					if (actions.has("show") && !text.contains("Wolfram") && actions.getJSONObject("show").has("images")) {
						log.trace("in Sraix.sraixPannous, found show action");
						final JSONArray arr = actions.getJSONObject("show").getJSONArray("images");
						final int i = (int) (arr.length() * Math.random());
						// for (int j = 0; j < arr.length(); j++) log.info(arr.getString(j));
						imgRef = arr.getString(i);
						if (imgRef.startsWith("//")) {
							imgRef = "http:" + imgRef;
						}
						imgRef = "<a href=\"" + imgRef + "\"><img src=\"" + imgRef + "\"/></a>";
						// log.info("IMAGE REF="+imgRef);

					}
					if (hint.equals(BotProperties.sraix_shopping_hint) && actions.has("open") && actions.getJSONObject("open").has("url")) {
						urlRef = "<oob><url>" + actions.getJSONObject("open").getString("url") + "</oob></url>";

					}
				}
				if (hint.equals(BotProperties.sraix_event_hint) && !text.startsWith("<year>")) {
					return BotProperties.sraix_failed;
				} else if (text.equals(BotProperties.sraix_failed)) {
					return AIMLProcessor.respond(BotProperties.sraix_failed, "nothing", "nothing", chatSession);
				} else {
					text = text.replace("&#39;", "'");
					text = text.replace("&apos;", "'");
					text = text.replaceAll("\\[(.*)\\]", "");
					String[] sentences;
					sentences = text.split("\\. ");
					// log.info("Sraix: text has "+sentences.length+" sentences:");
					String clippedPage = sentences[0];
					for (int i = 1; i < sentences.length; i++) {
						if (clippedPage.length() < 500) {
							clippedPage = clippedPage + ". " + sentences[i];
							// log.info(i+". "+sentences[i]);
						}
					}

					clippedPage = clippedPage + " " + imgRef + " " + urlRef;
					clippedPage = clippedPage.trim();
					log(rawInput, clippedPage);
					return clippedPage;
				}
			}
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
			log.info("Sraix '" + input + "' failed");
		}
		return BotProperties.sraix_failed;
	} // sraixPannous

	final static String NL = System.getProperty("line.separator");

	private static void log(String pattern, String template) {
		log.info("Logging " + pattern);
		template = template.trim();
		if (BotProperties.cache_sraix) {
			try {
				if (!template.contains("<year>") && !template.contains("No facilities")) {
					template = template.replace(NL, "\\#Newline");
					template = template.replace(",", "\\#Comma");
					template = template.replaceAll("<a(.*)</a>", "");
					template = template.trim();
					if (template.length() > 0) {
						final FileWriter fstream = new FileWriter("c:/ab/bots/sraixcache/aimlif/sraixcache.aiml.csv", true);
						final BufferedWriter fbw = new BufferedWriter(fstream);
						fbw.write("0," + pattern + ",*,*," + template + ",sraixcache.aiml");
						fbw.newLine();
						fbw.close();
					}
				}
			} catch (final Exception e) {
				log.info("Error: " + e.getMessage());
			}
		}
	}

}
