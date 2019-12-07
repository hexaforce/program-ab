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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.alicebot.ab.utils.CalendarUtils;
import org.alicebot.ab.utils.DomUtils;
import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.IntervalUtils;
import org.alicebot.ab.utils.JapaneseUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

/**
 * The core AIML parser and interpreter. Implements the AIML 2.1 specification
 * as described in AIML 2.1 Working Draft document
 * https://docs.google.com/document/d/1wNT25hJRyupcG51aO89UcQEiG-HkXRXusukADpFnDs4/pub
 */
@Slf4j
public
class AIMLProcessor {

	// static private boolean DEBUG = false;
	static AIMLProcessorExtension extension;

	/**
	 * when parsing an AIML file, process a category element.
	 *
	 * @param n          current XML parse node.
	 * @param categories list of categories found so far.
	 * @param topic      value of topic in case this category is wrapped in a
	 *                   {@code <topic>} tag
	 * @param aimlFile   name of AIML file being parsed.
	 */
	private static Category categoryProcessor(Node n, String topic, String aimlFile, String language) {
		String pattern, that, template;

		final NodeList children = n.getChildNodes();
		pattern = "*";
		that = "*";
		template = "";
		for (int j = 0; j < children.getLength(); j++) {
			// log.info("CHILD: " + children.item(j).getNodeName());
			final Node m = children.item(j);
			final String mName = m.getNodeName();
			// log.info("mName: " + mName);
			if (mName.equals("#text")) {
				/* skip */} else if (mName.equals("pattern")) {
				pattern = DomUtils.nodeToString(m);
			} else if (mName.equals("that")) {
				that = DomUtils.nodeToString(m);
			} else if (mName.equals("topic")) {
				topic = DomUtils.nodeToString(m);
			} else if (mName.equals("template")) {
				template = DomUtils.nodeToString(m);
			} else {
				log.info("categoryProcessor: unexpected " + mName + " in " + DomUtils.nodeToString(m));
			}
		}
		// log.info("categoryProcessor: pattern="+pattern);
		pattern = trimTag(pattern, "pattern");
		that = trimTag(that, "that");
		topic = trimTag(topic, "topic");
		pattern = cleanPattern(pattern);
		that = cleanPattern(that);
		topic = cleanPattern(topic);

		template = trimTag(template, "template");
		final String morphPattern = JapaneseUtils.tokenizeSentence(pattern);
		pattern = morphPattern;
		final String morphThatPattern = JapaneseUtils.tokenizeSentence(that);
		that = morphThatPattern;
		final String morphTopicPattern = JapaneseUtils.tokenizeSentence(topic);
		topic = morphTopicPattern;

		final Category c = new Category(0, pattern, that, topic, template, aimlFile);
		/*
		 * if (template == null) log.info("Template is null"); if (template.length()==0)
		 * log.info("Template is zero length");
		 */
		if (template == null || template.length() == 0) {
			log.info("Category " + c.inputThatTopic() + " discarded due to blank or missing <template>.");
		}
		return c;
	}

	private static String cleanPattern(String pattern) {
		pattern = pattern.replaceAll("(\r\n|\n\r|\r|\n)", " ");
		pattern = pattern.replaceAll("  ", " ");
		return pattern.trim();
	}

	public static String trimTag(String s, String tagName) {
		final String stag = "<" + tagName + ">";
		final String etag = "</" + tagName + ">";
		if (s.startsWith(stag) && s.endsWith(etag)) {
			s = s.substring(stag.length());
			s = s.substring(0, s.length() - etag.length());
		}
		return s.trim();
	}

	/**
	 * convert an AIML file to a list of categories.
	 *
	 * @param directory directory containing the AIML file.
	 * @param aimlFile  AIML file name.
	 * @return list of categories.
	 */
	static ArrayList<Category> AIMLToCategories(String aimlFileName, Node root) {
		try {
			final ArrayList<Category> categories = new ArrayList<Category>();
			final NodeList nodelist = root.getChildNodes();
			for (int i = 0; i < nodelist.getLength(); i++) {
				final Node n = nodelist.item(i);
				// log.info("AIML child: " +n.getNodeName());
				if (n.getNodeName().equals("category")) {
					final Category c = categoryProcessor(n, "*", aimlFileName, Properties.default_language);
					categories.add(c);
				} else if (n.getNodeName().equals("topic")) {
					final String topic = n.getAttributes().getNamedItem("name").getTextContent();
					// log.info("topic: " + topic);
					final NodeList children = n.getChildNodes();
					for (int j = 0; j < children.getLength(); j++) {
						final Node m = children.item(j);
						// log.info("Topic child: " + m.getNodeName());
						if (m.getNodeName().equals("category")) {
							final Category c = categoryProcessor(m, topic, aimlFileName, Properties.default_language);
							categories.add(c);
						}
					}
				}
			}
			return categories;
		} catch (final Exception ex) {
			log.info("AIMLToCategories: " + ex);
			log.error(ex.getMessage(), ex);
			return null;
		}
	}

	private static int sraiCount = 0;

	/**
	 * generate a bot response to a single sentence input.
	 *
	 * @param input       the input sentence.
	 * @param that        the bot's last sentence.
	 * @param topic       current topic.
	 * @param chatSession current client session.
	 * @return bot's response.
	 */
	static String respond(String input, String that, String topic, Chat chatSession) {
		return respond(input, that, topic, chatSession, 0);
	}

	/**
	 * generate a bot response to a single sentence input.
	 *
	 * @param input       input statement.
	 * @param that        bot's last reply.
	 * @param topic       current topic.
	 * @param chatSession current client chat session.
	 * @param srCnt       number of {@code <srai>} activations.
	 * @return bot's reply.
	 */
	private static String respond(String input, String that, String topic, Chat chatSession, int srCnt) {
		log.trace("input: " + input + ", that: " + that + ", topic: " + topic + ", chatSession: " + chatSession + ", srCnt: " + srCnt);
		String response;
		if (input == null || input.length() == 0) {
			input = Properties.null_input;
		}
		sraiCount = srCnt;
		response = Properties.default_bot_response;
		try {
			final Nodemapper leaf = chatSession.bot.brain.match(input, that, topic);
			if (leaf == null) {
				return (response);
			}
			final ParseState ps = new ParseState(0, chatSession, input, that, topic, leaf, new Predicates(), leaf.starBindings);
			// chatSession.matchTrace += leaf.category.getTemplate()+"\n";
			final String template = leaf.category.getTemplate();
			// Properties.trace("in AIMLProcessor.respond(), template: " + template);
			response = evalTemplate(template, ps);
			// log.info("That="+that);
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return response;
	}

	/**
	 * capitalizeString: from
	 * http://stackoverflow.com/questions/1892765/capitalize-first-char-of-each-word-in-a-string-java
	 *
	 * @param string the string to capitalize
	 * @return the capitalized string
	 */

	private static String capitalizeString(String string) {
		final char[] chars = string.toLowerCase().toCharArray();
		boolean found = false;
		for (int i = 0; i < chars.length; i++) {
			if (!found && Character.isLetter(chars[i])) {
				chars[i] = Character.toUpperCase(chars[i]);
				found = true;
			} else if (Character.isWhitespace(chars[i])) {
				found = false;
			}
		}
		return String.valueOf(chars);
	}

	/**
	 * explode a string into individual characters separated by one space
	 *
	 * @param input input string
	 * @return exploded string
	 */
	private static String explode(String input) {
		String result = "";
		for (int i = 0; i < input.length(); i++) {
			result += " " + input.charAt(i);
		}
		while (result.contains("  ")) {
			result = result.replace("  ", " ");
		}
		return result.trim();
	}

	// Parsing and evaluation functions:

	/**
	 * evaluate the contents of an AIML tag. calls recursEval on child tags.
	 *
	 * @param node             the current parse node.
	 * @param ps               the current parse state.
	 * @param ignoreAttributes tag names to ignore when evaluating the tag.
	 * @return the result of evaluating the tag contents.
	 */
	static String evalTagContent(Node node, ParseState ps, Set<String> ignoreAttributes) {
		// Properties.trace("AIMLProcessor.evalTagContent(node: " + node + ", ps: " +
		// ps + ", ignoreAttributes: " + ignoreAttributes);
		// Properties.trace("in AIMLProcessor.evalTagContent, node string: " +
		// DomUtils.nodeToString(node));
		String result = "";
		try {
			final NodeList childList = node.getChildNodes();
			for (int i = 0; i < childList.getLength(); i++) {
				final Node child = childList.item(i);
				// Properties.trace("in AIMLProcessor.evalTagContent(), child: " + child);
				if (ignoreAttributes == null || !ignoreAttributes.contains(child.getNodeName())) {
					result += recursEval(child, ps);
					// Properties.trace("in AIMLProcessor.evalTagContent(), result: " + result);
				}
			}
		} catch (final Exception ex) {
			log.info("Something went wrong with evalTagContent");
			log.error(ex.getMessage(), ex);
		}
		// Properties.trace("AIMLProcessor.evalTagContent() returning: " + result);
		return result;
	}

	/**
	 * pass thru generic XML (non-AIML tags, such as HTML) as unevaluated XML
	 *
	 * @param node current parse node
	 * @param ps   current parse state
	 * @return unevaluated generic XML string
	 */
	static String genericXML(Node node, ParseState ps) {
		// Properties.trace("AIMLProcessor.genericXML(node: " + node + ", ps: " +
		// ps);
		final String evalResult = evalTagContent(node, ps, null);
		final String result = unevaluatedXML(evalResult, node, ps);
		// Properties.trace("in AIMLProcessor.genericXML(), returning: " + result);
		return result;
	}

	/**
	 * return a string of unevaluated XML. When the AIML parser encounters an
	 * unrecognized XML tag, it simply passes through the tag in XML form. For
	 * example, if the response contains HTML markup, the HTML is passed to the
	 * requesting process. However if that markup contains AIML tags, those tags are
	 * evaluated and the parser builds the result.
	 *
	 * @param node current parse node.
	 * @param ps   current parse state.
	 * @return the unevaluated XML string
	 */
	private static String unevaluatedXML(String resultIn, Node node, ParseState ps) {
		// Properties.trace("AIMLProcessor.unevaluatedXML(resultIn: " + resultIn + ",
		// node: " + node + ", ps: " + ps);
		final String nodeName = node.getNodeName();
		// Properties.trace("in AIMLProcessor.unevaluatedXML(), nodeName: " +
		// nodeName);
		String attributes = "";
		if (node.hasAttributes()) {
			final NamedNodeMap XMLAttributes = node.getAttributes();
			for (int i = 0; i < XMLAttributes.getLength(); i++) {
				attributes += " " + XMLAttributes.item(i).getNodeName() + "=\"" + XMLAttributes.item(i).getNodeValue() + "\"";
			}
		}
		// String contents = evalTagContent(node, ps, null);
		String result = "<" + nodeName + attributes + "/>";
		if (!resultIn.equals("")) {
			result = "<" + nodeName + attributes + ">" + resultIn + "</" + nodeName + ">";
		}
		// Properties.trace("in AIMLProcessor.unevaluatedXML() returning: " +
		// result);
		return result;
	}

	static int trace_count = 0;

	/**
	 * implements AIML <srai> tag
	 *
	 * @param node current parse node.
	 * @param ps   current parse state.
	 * @return the result of processing the <srai>
	 *
	 */
	private static String srai(Node node, ParseState ps) {
		// Properties.trace("AIMLProcessor.srai(node: " + node + ", ps: " + ps);
		sraiCount++;
		if (sraiCount > Properties.max_recursion_count || ps.depth > Properties.max_recursion_depth) {
			return Properties.too_much_recursion;
		}
		String response = Properties.default_bot_response;
		try {
			String result = evalTagContent(node, ps, null);
			result = result.trim();
			result = result.replaceAll("(\r\n|\n\r|\r|\n)", " ");
			result = ps.chatSession.bot.preProcessor.normalize(result);
			result = JapaneseUtils.tokenizeSentence(result);
			final String topic = ps.chatSession.predicates.get("topic"); // the that stays the same, but the topic may
																			// have changed
			if (log.isDebugEnabled()) {
				log.debug(trace_count + ". <srai>" + result + "</srai> from " + ps.leaf.category.inputThatTopic() + " topic=" + topic + ") ");
				trace_count++;
			}
			final Nodemapper leaf = ps.chatSession.bot.brain.match(result, ps.that, topic);
			if (leaf == null) {
				return (response);
			}
			// log.info("Srai returned
			// "+leaf.category.inputThatTopic()+":"+leaf.category.getTemplate());
			response = evalTemplate(leaf.category.getTemplate(), new ParseState(ps.depth + 1, ps.chatSession, ps.input, ps.that, topic, leaf, new Predicates(), leaf.starBindings));
			// log.info("That="+that);
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		final String result = response.trim();
		// Properties.trace("in AIMLProcessor.srai(), returning: " + result);
		return result;
	}

	/**
	 * in AIML 2.1, an attribute value can be specified by either an XML attribute
	 * value or a subtag of the same name. This function tries to read the value
	 * from the XML attribute first, then tries to look for the subtag.
	 *
	 * @param node          current parse node.
	 * @param ps            current parse state.
	 * @param attributeName the name of the attribute.
	 * @return the attribute value.
	 */
	// value can be specified by either attribute or tag
	private static String getAttributeOrTagValue(Node node, ParseState ps, String attributeName) { // AIML 2.1
		// Properties.trace("AIMLProcessor.getAttributeOrTagValue (node: " + node +
		// ", attributeName: " + attributeName + ")");
		String result = "";
		final Node m = node.getAttributes().getNamedItem(attributeName);
		if (m == null) {
			final NodeList childList = node.getChildNodes();
			result = null; // no attribute or tag named attributeName
			for (int i = 0; i < childList.getLength(); i++) {
				final Node child = childList.item(i);
				// log.info("getAttributeOrTagValue child = "+child.getNodeName());
				if (child.getNodeName().equals(attributeName)) {
					result = evalTagContent(child, ps, null);
					// log.info("getAttributeOrTagValue result from child = "+result);
				}
			}
		} else {
			result = m.getNodeValue();
		}
		// Properties.trace("in AIMLProcessor.getAttributeOrTagValue (), returning: "
		// + result);
		return result;
	}

	/**
	 * access external web service for response implements <sraix></sraix> and its
	 * attribute variations.
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return response from remote service or string indicating failure.
	 */
	private static String sraix(Node node, ParseState ps) {
		final HashSet<String> attributeNames = Utilities.stringSet("botid", "host");
		final String host = getAttributeOrTagValue(node, ps, "host");
		final String botid = getAttributeOrTagValue(node, ps, "botid");
		final String hint = getAttributeOrTagValue(node, ps, "hint");
		final String limit = getAttributeOrTagValue(node, ps, "limit");
		final String defaultResponse = getAttributeOrTagValue(node, ps, "default");
		final String evalResult = evalTagContent(node, ps, attributeNames);
		final String result = Sraix.sraix(ps.chatSession, evalResult, defaultResponse, hint, host, botid, null, limit);
		return result;
	}

	/**
	 * map an element of one string set to an element of another Implements
	 * <map name="mapname"></map> and <map><name>mapname</name></map>
	 *
	 * @param node current XML parse node
	 * @param ps   current AIML parse state
	 * @return the map result or a string indicating the key was not found
	 */
	private static String map(Node node, ParseState ps) {
		String result = Properties.default_map;
		final HashSet<String> attributeNames = Utilities.stringSet("name");
		final String mapName = getAttributeOrTagValue(node, ps, "name");
		String contents = evalTagContent(node, ps, attributeNames);
		contents = contents.trim();
		if (mapName == null) {
			result = "<map>" + contents + "</map>"; // this is an OOB map tag (no attribute)
		} else {
			final AIMLMap map = ps.chatSession.bot.mapMap.get(mapName);
			if (map != null) {
				result = map.get(contents.toUpperCase());
			}
			// log.info("AIMLProcessor map "+contents+" "+result);
			if (result == null) {
				result = Properties.default_map;
			}
			result = result.trim();
		}
		return result;
	}

	/**
	 * set the value of an AIML predicate. Implements <set name="predicate"></set>
	 * and <set var="varname"></set>
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the result of the <set> operation
	 */
	private static String set(Node node, ParseState ps) { // add pronoun check
		// Properties.trace("AIMLProcessor.set(node: " + node + ", ps: " + ps + ")");
		final HashSet<String> attributeNames = Utilities.stringSet("name", "var");
		final String predicateName = getAttributeOrTagValue(node, ps, "name");
		final String varName = getAttributeOrTagValue(node, ps, "var");
		String result = evalTagContent(node, ps, attributeNames).trim();
		result = result.replaceAll("(\r\n|\n\r|\r|\n)", " ");
		final String value = result.trim();
		if (predicateName != null) {
			ps.chatSession.predicates.put(predicateName, result);
			log.trace("Set predicate " + predicateName + " to " + result + " in " + ps.leaf.category.inputThatTopic());
		}
		if (varName != null) {
			ps.vars.put(varName, result);
			log.trace("Set var " + varName + " to " + value + " in " + ps.leaf.category.inputThatTopic());
		}
		if (ps.chatSession.bot.pronounSet.contains(predicateName)) {
			result = predicateName;
		}
		// Properties.trace("in AIMLProcessor.set, returning: " + result);
		return result;
	}

	/**
	 * get the value of an AIML predicate. implements <get name="predicate"></get>
	 * and <get var="varname"></get>
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the result of the <get> operation
	 */
	private static String get(Node node, ParseState ps) {
		// Properties.trace("AIMLProcessor.get(node: " + node + ", ps: " + ps + ")");
		String result = Properties.default_get;
		final String predicateName = getAttributeOrTagValue(node, ps, "name");
		final String varName = getAttributeOrTagValue(node, ps, "var");
		final String tupleName = getAttributeOrTagValue(node, ps, "tuple");
		if (predicateName != null) {
			result = ps.chatSession.predicates.get(predicateName).trim();
		} else if (varName != null && tupleName != null) {
			result = tupleGet(tupleName, varName);

		} else if (varName != null) {
			result = ps.vars.get(varName).trim();
		}
		// Properties.trace("in AIMLProcessor.get, returning: " + result);
		return result;
	}

	private static String tupleGet(String tupleName, String varName) {
		String result = Properties.default_get;
		final Tuple tuple = Tuple.tupleMap.get(tupleName);
		// log.info("Tuple = "+tuple.printTuple());
		// log.info("Value = "+tuple.getValue(varName));
		if (tuple == null) {
			result = Properties.default_get;
		} else {
			result = tuple.getValue(varName);
		}
		return result;
	}

	/**
	 * return the value of a bot property. implements
	 * {{{@code <bot name="property"/>}}}
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the bot property or a string indicating the property was not found.
	 */
	private static String bot(Node node, ParseState ps) {
		String result = Properties.default_property;
		// HashSet<String> attributeNames = Utilities.stringSet("name");
		final String propertyName = getAttributeOrTagValue(node, ps, "name");
		if (propertyName != null) {
			result = ps.chatSession.bot.properties.get(propertyName).trim();
		}
		// log.info("BOT: "+m.getNodeValue()+"="+result);
		return result;
	}

	/**
	 * implements formatted date tag <date jformat="format"/> and
	 * <date format="format"/>
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the formatted date
	 */
	private static String date(Node node, ParseState ps) {
		// HashSet<String> attributeNames =
		// Utilities.stringSet("jformat","format","locale","timezone");
		final String jformat = getAttributeOrTagValue(node, ps, "jformat"); // AIML 2.1
		final String locale = getAttributeOrTagValue(node, ps, "locale");
		final String timezone = getAttributeOrTagValue(node, ps, "timezone");
		// log.info("Format = "+format+" Locale = "+locale+" Timezone =
		// "+timezone);
		final String dateAsString = CalendarUtils.date(jformat, locale, timezone);
		// log.info(dateAsString);
		return dateAsString;
	}

	/**
	 * <interval><style>years</style></style><jformat>MMMMMMMMM dd,
	 * yyyy</jformat><from>August 2, 1960</from><to><date><jformat>MMMMMMMMM dd,
	 * yyyy</jformat></date></to></interval>
	 */

	private static String interval(Node node, ParseState ps) {
		// HashSet<String> attributeNames =
		// Utilities.stringSet("style","jformat","from","to");
		String style = getAttributeOrTagValue(node, ps, "style"); // AIML 2.1
		String jformat = getAttributeOrTagValue(node, ps, "jformat"); // AIML 2.1
		String from = getAttributeOrTagValue(node, ps, "from");
		String to = getAttributeOrTagValue(node, ps, "to");
		if (style == null) {
			style = "years";
		}
		if (jformat == null) {
			jformat = "MMMMMMMMM dd, yyyy";
		}
		if (from == null) {
			from = "January 1, 1970";
		}
		if (to == null) {
			to = CalendarUtils.date(jformat, null, null);
		}
		String result = "unknown";
		if (style.equals("years")) {
			result = "" + IntervalUtils.getYearsBetween(from, to, jformat);
		}
		if (style.equals("months")) {
			result = "" + IntervalUtils.getMonthsBetween(from, to, jformat);
		}
		if (style.equals("days")) {
			result = "" + IntervalUtils.getDaysBetween(from, to, jformat);
		}
		if (style.equals("hours")) {
			result = "" + IntervalUtils.getHoursBetween(from, to, jformat);
		}
		return result;
	}

	/**
	 * get the value of an index attribute and return it as an integer. if it is not
	 * recognized as an integer, return 0
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the the integer intex value
	 */
	private static int getIndexValue(Node node, ParseState ps) {
		int index = 0;
		final String value = getAttributeOrTagValue(node, ps, "index");
		if (value != null) {
			try {
				index = Integer.parseInt(value) - 1;
			} catch (final Exception ex) {
				log.error(ex.getMessage(), ex);
			}
		}
		return index;
	}

	/**
	 * implements {@code <star index="N"/>} returns the value of input words
	 * matching the Nth wildcard (or AIML Set).
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the word sequence matching a wildcard
	 */
	private static String inputStar(Node node, ParseState ps) {
		String result = "";
		final int index = getIndexValue(node, ps);
		if (ps.starBindings.inputStars.star(index) == null) {
			result = "";
		} else {
			result = ps.starBindings.inputStars.star(index).trim();
		}
		// log.info("inputStar: ps depth="+ps.depth+" index="+index+"
		// star="+result);
		return result;
	}

	/**
	 * implements {@code <thatstar index="N"/>} returns the value of input words
	 * matching the Nth wildcard (or AIML Set) in <that></that>.
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the word sequence matching a wildcard
	 */
	private static String thatStar(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		if (ps.starBindings.thatStars.star(index) == null) {
			return "";
		} else {
			return ps.starBindings.thatStars.star(index).trim();
		}
	}

	/**
	 * implements <topicstar/> and <topicstar index="N"/> returns the value of input
	 * words matching the Nth wildcard (or AIML Set) in a topic pattern.
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the word sequence matching a wildcard
	 */
	private static String topicStar(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		if (ps.starBindings.topicStars.star(index) == null) {
			return "";
		} else {
			return ps.starBindings.topicStars.star(index).trim();
		}
	}

	/**
	 * return the client ID. implements {@code <id/>}
	 *
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return client ID
	 */

	private static String id(Node node, ParseState ps) {
		return ps.chatSession.customerId;
	}

	/**
	 * return the size of the robot brain (number of AIML categories loaded).
	 * implements {@code <size/>}
	 *
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return bot brain size
	 */
	private static String size(Node node, ParseState ps) {
		final int size = ps.chatSession.bot.brain.getCategories().size();
		return String.valueOf(size);
	}

	/**
	 * return the size of the robot vocabulary (number of words the bot can
	 * recognize). implements {@code <vocabulary/>}
	 *
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return bot vocabulary size
	 */
	private static String vocabulary(Node node, ParseState ps) {
		final int size = ps.chatSession.bot.brain.getVocabulary().size();
		return String.valueOf(size);
	}

	/**
	 * return a string indicating the name and version of the AIML program.
	 * implements {@code <program/>}
	 *
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return AIML program name and version.
	 */
	private static String program(Node node, ParseState ps) {
		return Properties.program_name_version;
	}

	/**
	 * implements the (template-side) {@code <that index="M,N"/>} tag. returns a
	 * normalized sentence.
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the nth last sentence of the bot's mth last reply.
	 */
	private static String that(Node node, ParseState ps) {
		int index = 0;
		int jndex = 0;
		final String value = getAttributeOrTagValue(node, ps, "index");
		if (value != null) {
			try {
				final String pair = value;
				final String[] spair = pair.split(",");
				index = Integer.parseInt(spair[0]) - 1;
				jndex = Integer.parseInt(spair[1]) - 1;
				log.info("That index=" + index + "," + jndex);
			} catch (final Exception ex) {
				log.error(ex.getMessage(), ex);
			}
		}
		String that = Properties.unknown_history_item;
		final History<?> hist = ps.chatSession.thatHistory.get(index);
		if (hist != null) {
			that = (String) hist.get(jndex);
		}
		return that.trim();
	}

	/**
	 * implements {@code <input index="N"/>} tag
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the nth last sentence input to the bot
	 */

	private static String input(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		return ps.chatSession.inputHistory.getString(index);
	}

	/**
	 * implements {@code <request index="N"/>} tag
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the nth last multi-sentence request to the bot.
	 */
	private static String request(Node node, ParseState ps) { // AIML 2.1
		final int index = getIndexValue(node, ps);
		return ps.chatSession.requestHistory.getString(index).trim();
	}

	/**
	 * implements {@code <response index="N"/>} tag
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the bot's Nth last multi-sentence response.
	 */
	private static String response(Node node, ParseState ps) { // AIML 2.1
		final int index = getIndexValue(node, ps);
		return ps.chatSession.responseHistory.getString(index).trim();
	}

	/**
	 * implements {@code <system>} tag. Evaluate the contents, and try to execute
	 * the result as a command in the underlying OS shell. Read back and return the
	 * result of this command.
	 *
	 * The timeout parameter allows the botmaster to set a timeout in ms, so that
	 * the <system></system> command returns eventually.
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return the result of executing the system command or a string indicating the
	 *         command failed.
	 */
	private static String system(Node node, ParseState ps) {
		final HashSet<String> attributeNames = Utilities.stringSet("timeout");
		// String stimeout = getAttributeOrTagValue(node, ps, "timeout");
		final String evaluatedContents = evalTagContent(node, ps, attributeNames);
		final String result = IOUtils.system(evaluatedContents, Properties.system_failed);
		return result;
	}

	/**
	 * implements {@code <think>} tag
	 *
	 * Evaluate the tag contents but return a blank. "Think but don't speak."
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return a blank empty string
	 */
	private static String think(Node node, ParseState ps) {
		evalTagContent(node, ps, null);
		return "";
	}

	/**
	 * Transform a string of words (separtaed by spaces) into a string of individual
	 * characters (separated by spaces). Explode "ABC DEF" = "A B C D E F".
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return exploded string
	 */
	private static String explode(Node node, ParseState ps) { // AIML 2.1
		final String result = evalTagContent(node, ps, null);
		return explode(result);
	}

	/**
	 * apply the AIML normalization pre-processor to the evaluated tag contenst.
	 * implements {@code <normalize>} tag.
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return normalized string
	 */
	private static String normalize(Node node, ParseState ps) { // AIML 2.1
		// Properties.trace("AIMLPreprocessor.normalize(node: " + node + ", ps: " +
		// ")");
		final String result = evalTagContent(node, ps, null);
		// Properties.trace("in AIMLPreprocessor.normalize(), result: " + result);
		final String returning = ps.chatSession.bot.preProcessor.normalize(result);
		// Properties.trace("in AIMLPreprocessor.normalize(), returning: " +
		// returning);
		return returning;
	}

	/**
	 * apply the AIML denormalization pre-processor to the evaluated tag contenst.
	 * implements {@code <normalize>} tag.
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return denormalized string
	 */
	private static String denormalize(Node node, ParseState ps) { // AIML 2.1
		final String result = evalTagContent(node, ps, null);
		return ps.chatSession.bot.preProcessor.denormalize(result);
	}

	/**
	 * evaluate tag contents and return result in upper case implements
	 * {@code <uppercase>} tag
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return uppercase string
	 */
	private static String uppercase(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return result.toUpperCase();
	}

	/**
	 * evaluate tag contents and return result in lower case implements
	 * {@code <lowercase>} tag
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return lowercase string
	 */
	private static String lowercase(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return result.toLowerCase();
	}

	/**
	 * evaluate tag contents and capitalize each word. implements {@code <formal>}
	 * tag
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return capitalized string
	 */
	private static String formal(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return capitalizeString(result);
	}

	/**
	 * evaluate tag contents and capitalize the first word. implements
	 * {@code <sentence>} tag
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return string with first word capitalized
	 */
	private static String sentence(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		if (result.length() > 1) {
			return result.substring(0, 1).toUpperCase() + result.substring(1, result.length());
		} else {
			return "";
		}
	}

	/**
	 * evaluate tag contents and swap 1st and 2nd person pronouns implements
	 * {@code <person>} tag
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return sentence with pronouns swapped
	 */
	private static String person(Node node, ParseState ps) {
		String result;
		if (node.hasChildNodes()) {
			result = evalTagContent(node, ps, null);
		} else {
			result = ps.starBindings.inputStars.star(0); // for <person/>
		}
		result = " " + result + " ";
		result = ps.chatSession.bot.preProcessor.person(result);
		return result.trim();
	}

	/**
	 * evaluate tag contents and swap 1st and 3rd person pronouns implements
	 * {@code <person2>} tag
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return sentence with pronouns swapped
	 */
	private static String person2(Node node, ParseState ps) {
		String result;
		if (node.hasChildNodes()) {
			result = evalTagContent(node, ps, null);
		} else {
			result = ps.starBindings.inputStars.star(0); // for <person2/>
		}
		result = " " + result + " ";
		result = ps.chatSession.bot.preProcessor.person2(result);
		return result.trim();
	}

	/**
	 * implements {@code <gender>} tag swaps gender pronouns
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return sentence with gender ronouns swapped
	 */
	private static String gender(Node node, ParseState ps) {
		String result = evalTagContent(node, ps, null);
		result = " " + result + " ";
		result = ps.chatSession.bot.preProcessor.gender(result);
		return result.trim();
	}

	/**
	 * implements {@code <random>} tag
	 * 
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return response randomly selected from the list
	 */
	private static String random(Node node, ParseState ps) {
		final NodeList childList = node.getChildNodes();
		final ArrayList<Node> liList = new ArrayList<Node>();
		@SuppressWarnings("unused")
		final String setName = getAttributeOrTagValue(node, ps, "set");
		for (int i = 0; i < childList.getLength(); i++) {
			if (childList.item(i).getNodeName().equals("li")) {
				liList.add(childList.item(i));
			}
		}
		int index = (int) (Math.random() * liList.size());
		if (Properties.qa_test_mode) {
			index = 0;
		}
		return evalTagContent(liList.get(index), ps, null);
	}

	private static String unevaluatedAIML(Node node, ParseState ps) {
		final String result = learnEvalTagContent(node, ps);
		return unevaluatedXML(result, node, ps);
	}

	private static String recursLearn(Node node, ParseState ps) {
		final String nodeName = node.getNodeName();
		if (nodeName.equals("#text")) {
			return node.getNodeValue();
		} else if (nodeName.equals("eval")) {
			return evalTagContent(node, ps, null); // AIML 2.1
		} else {
			return unevaluatedAIML(node, ps);
		}
	}

	private static String learnEvalTagContent(Node node, ParseState ps) {
		String result = "";
		final NodeList childList = node.getChildNodes();
		for (int i = 0; i < childList.getLength(); i++) {
			final Node child = childList.item(i);
			result += recursLearn(child, ps);
		}
		return result;
	}

	final static String NL = System.getProperty("line.separator");

	private static String learn(Node node, ParseState ps) { // learn, learnf AIML 2.1
		final NodeList childList = node.getChildNodes();
		String pattern = "";
		String that = "*";
		String template = "";
		for (int i = 0; i < childList.getLength(); i++) {
			if (childList.item(i).getNodeName().equals("category")) {
				final NodeList grandChildList = childList.item(i).getChildNodes();
				for (int j = 0; j < grandChildList.getLength(); j++) {
					if (grandChildList.item(j).getNodeName().equals("pattern")) {
						pattern = recursLearn(grandChildList.item(j), ps);
					} else if (grandChildList.item(j).getNodeName().equals("that")) {
						that = recursLearn(grandChildList.item(j), ps);
					} else if (grandChildList.item(j).getNodeName().equals("template")) {
						template = recursLearn(grandChildList.item(j), ps);
					}
				}
				pattern = pattern.substring("<pattern>".length(), pattern.length() - "</pattern>".length());
				log.debug("Learn Pattern = " + pattern);
				if (template.length() >= "<template></template>".length()) {
					template = template.substring("<template>".length(), template.length() - "</template>".length());
				}
				if (that.length() >= "<that></that>".length()) {
					that = that.substring("<that>".length(), that.length() - "</that>".length());
				}
				pattern = pattern.toUpperCase();
				pattern = pattern.replaceAll(NL, " ");
				pattern = pattern.replaceAll("[ ]+", " ");
				that = that.toUpperCase();
				that = that.replaceAll(NL, " ");
				that = that.replaceAll("[ ]+", " ");
				log.debug("Learn Pattern = " + pattern);
				log.debug("Learn That = " + that);
				log.debug("Learn Template = " + template);
				Category c;
				if (node.getNodeName().equals("learn")) {
					// log.info("node is <learn>");
					c = new Category(0, pattern, that, "*", template, Properties.null_aiml_file);
					ps.chatSession.bot.learnGraph.addCategory(c);
				} else {// learnf
						// log.info("node is <learnf>");
					c = new Category(0, pattern, that, "*", template, Properties.learnf_aiml_file);
					ps.chatSession.bot.learnfGraph.addCategory(c);
				}
				ps.chatSession.bot.brain.addCategory(c);
				// ps.chatSession.bot.brain.printgraph();
			}
		}
		return "";
	}

	/**
	 * implements {@code <condition> with <loop/>} re-evaluate the conditional
	 * statement until the response does not contain {@code <loop/>}
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return result of conditional expression
	 */
	private static String loopCondition(Node node, ParseState ps) {
		boolean loop = true;
		String result = "";
		final int loopCnt = 0;
		while (loop && loopCnt < Properties.max_loops) {
			String loopResult = condition(node, ps);
			if (loopResult.trim().equals(Properties.too_much_recursion)) {
				return Properties.too_much_recursion;
			}
			if (loopResult.contains("<loop/>")) {
				loopResult = loopResult.replace("<loop/>", "");
				loop = true;
			} else {
				loop = false;
			}
			result += loopResult;
		}
		if (loopCnt >= Properties.max_loops) {
			result = Properties.too_much_looping;
		}
		return result;
	}

	/**
	 * implements all 3 forms of the {@code <condition> tag} In AIML 2.1 the
	 * conditional may return a {@code <loop/>}
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 * @return result of conditional expression
	 */
	private static String condition(Node node, ParseState ps) {
		final String result = "";
		// boolean loop = true;
		final NodeList childList = node.getChildNodes();
		final ArrayList<Node> liList = new ArrayList<Node>();
		String predicate = null, varName = null, value = null; // Node p=null, v=null;
		final HashSet<String> attributeNames = Utilities.stringSet("name", "var", "value");
		// First check if the <condition> has an attribute "name". If so, get the
		// predicate name.
		predicate = getAttributeOrTagValue(node, ps, "name");
		varName = getAttributeOrTagValue(node, ps, "var");
		// Make a list of all the <li> child nodes:
		for (int i = 0; i < childList.getLength(); i++) {
			if (childList.item(i).getNodeName().equals("li")) {
				liList.add(childList.item(i));
			}
		}
		// if there are no <li> nodes, this is a one-shot condition.
		if (liList.size() == 0 && (value = getAttributeOrTagValue(node, ps, "value")) != null && predicate != null && ps.chatSession.predicates.get(predicate).equalsIgnoreCase(value)) {
			return evalTagContent(node, ps, attributeNames);
		} else if (liList.size() == 0 && (value = getAttributeOrTagValue(node, ps, "value")) != null && varName != null && ps.vars.get(varName).equalsIgnoreCase(value)) {
			return evalTagContent(node, ps, attributeNames);
		}
		// otherwise this is a <condition> with <li> items:
		else {
			for (int i = 0; i < liList.size() && result.equals(""); i++) {
				final Node n = liList.get(i);
				String liPredicate = predicate;
				String liVarName = varName;
				if (liPredicate == null) {
					liPredicate = getAttributeOrTagValue(n, ps, "name");
				}
				if (liVarName == null) {
					liVarName = getAttributeOrTagValue(n, ps, "var");
				}
				value = getAttributeOrTagValue(n, ps, "value");
				// log.info("condition name="+liPredicate+" value="+value);
				if (value != null) {
					// if the predicate equals the value, return the <li> item.
					if (liPredicate != null && value != null && (ps.chatSession.predicates.get(liPredicate).equalsIgnoreCase(value) || (ps.chatSession.predicates.containsKey(liPredicate) && value.equals("*")))) {
						return evalTagContent(n, ps, attributeNames);
					} else if (liVarName != null && value != null && (ps.vars.get(liVarName).equalsIgnoreCase(value) || (ps.vars.containsKey(liPredicate) && value.equals("*")))) {
						return evalTagContent(n, ps, attributeNames);
					}

				} else {
					// condition.
					return evalTagContent(n, ps, attributeNames);
				}
			}
		}
		return "";

	}

	private static String deleteTriple(Node node, ParseState ps) {
		final String subject = getAttributeOrTagValue(node, ps, "subj");
		final String predicate = getAttributeOrTagValue(node, ps, "pred");
		final String object = getAttributeOrTagValue(node, ps, "obj");
		return ps.chatSession.tripleStore.deleteTriple(subject, predicate, object);
	}

	private static String addTriple(Node node, ParseState ps) {
		final String subject = getAttributeOrTagValue(node, ps, "subj");
		final String predicate = getAttributeOrTagValue(node, ps, "pred");
		final String object = getAttributeOrTagValue(node, ps, "obj");
		return ps.chatSession.tripleStore.addTriple(subject, predicate, object);
	}

	/*
	 * private static String qnotq(Node node, ParseState ps, Boolean affirm) {
	 * String subject = getAttributeOrTagValue(node, ps, "subj"); String predicate =
	 * getAttributeOrTagValue(node, ps, "pred"); String object =
	 * getAttributeOrTagValue(node, ps, "obj"); TripleStore ts =
	 * ps.chatSession.tripleStore; HashSet<String> triples = ts.getTriples(subject,
	 * predicate, object); if (!affirm) { HashSet<String> U = new
	 * HashSet<String>(ts.allTriples()); U.removeAll(triples); triples = U; } return
	 * ts.formatAIMLTripleList(triples); } private static String q(Node node,
	 * ParseState ps) { return qnotq(node, ps, true); } private static String
	 * notq(Node node, ParseState ps) { return qnotq(node, ps, false); }
	 */
	private static String uniq(Node node, ParseState ps) {
		final HashSet<String> vars = new HashSet<String>();
		final HashSet<String> visibleVars = new HashSet<String>();
		String subj = "?subject";
		String pred = "?predicate";
		String obj = "?object";
		final NodeList childList = node.getChildNodes();
		for (int j = 0; j < childList.getLength(); j++) {
			final Node childNode = childList.item(j);
			final String contents = evalTagContent(childNode, ps, null);
			if (childNode.getNodeName().equals("subj")) {
				subj = contents;
			} else if (childNode.getNodeName().equals("pred")) {
				pred = contents;
			} else if (childNode.getNodeName().equals("obj")) {
				obj = contents;
			}
			if (contents.startsWith("?")) {
				visibleVars.add(contents);
				vars.add(contents);
			}
		}
		final Tuple partial = new Tuple(vars, visibleVars, null);
		final Clause clause = new Clause(subj, pred, obj);
		final HashSet<Tuple> tuples = ps.chatSession.tripleStore.selectFromSingleClause(partial, clause, true);
		String tupleList = "";
		for (final Tuple tuple : tuples) {
			tupleList = tuple.name + " " + tupleList;
		}
		tupleList = tupleList.trim();
		if (tupleList.length() == 0) {
			tupleList = "NIL";
		}
		String var = "";
		for (final String x : visibleVars) {
			var = x;
		}
		final String firstTuple = firstWord(tupleList);
		final String result = tupleGet(firstTuple, var);
		return result;
	}

	private static String select(Node node, ParseState ps) {
		final ArrayList<Clause> clauses = new ArrayList<Clause>();
		final NodeList childList = node.getChildNodes();
		// String[] splitTuple;
		final HashSet<String> vars = new HashSet<String>();
		final HashSet<String> visibleVars = new HashSet<String>();
		for (int i = 0; i < childList.getLength(); i++) {
			final Node childNode = childList.item(i);
			if (childNode.getNodeName().equals("vars")) {
				final String contents = evalTagContent(childNode, ps, null);
				final String[] splitVars = contents.split(" ");
				for (String var : splitVars) {
					var = var.trim();
					if (var.length() > 0) {
						visibleVars.add(var);
					}
				}
				// log.info("AIML Processor select: visible vars "+visibleVars);
			}

			else if (childNode.getNodeName().equals("q") || childNode.getNodeName().equals("notq")) {
				final Boolean affirm = !childNode.getNodeName().equals("notq");
				final NodeList grandChildList = childNode.getChildNodes();
				String subj = null;
				String pred = null;
				String obj = null;
				for (int j = 0; j < grandChildList.getLength(); j++) {
					final Node grandChildNode = grandChildList.item(j);
					final String contents = evalTagContent(grandChildNode, ps, null);
					if (grandChildNode.getNodeName().equals("subj")) {
						subj = contents;
					} else if (grandChildNode.getNodeName().equals("pred")) {
						pred = contents;
					} else if (grandChildNode.getNodeName().equals("obj")) {
						obj = contents;
					}
					if (contents.startsWith("?")) {
						vars.add(contents);
					}

				}
				final Clause clause = new Clause(subj, pred, obj, affirm);
				// log.info("Vars "+vars+" Clause "+subj+" "+pred+" "+obj+" "+affirm);
				clauses.add(clause);

			}
		}
		final HashSet<Tuple> tuples = ps.chatSession.tripleStore.select(vars, visibleVars, clauses);
		String result = "";
		for (final Tuple tuple : tuples) {
			result = tuple.name + " " + result;
		}
		result = result.trim();
		if (result.length() == 0) {
			result = "NIL";
		}
		return result;
	}

	private static String javascript(Node node, ParseState ps) {
		// Properties.trace("AIMLProcessor.javascript(node: " + node + ", ps: " + ps
		// + ")");
		String result = Properties.bad_javascript;
		final String script = evalTagContent(node, ps, null);

		try {
			result = IOUtils.evalScript("JavaScript", script);
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		log.trace("in AIMLProcessor.javascript, returning result: " + result);
		return result;
	}

	private static String firstWord(String sentence) {
		String content = (sentence == null ? "" : sentence);
		content = content.trim();
		if (content.contains(" ")) {
			final String head = content.substring(0, content.indexOf(" "));
			return head;
		} else if (content.length() > 0) {
			return content;
		} else {
			return Properties.default_list_item;
		}
	}

	private static String restWords(String sentence) {
		String content = (sentence == null ? "" : sentence);
		content = content.trim();
		if (content.contains(" ")) {
			final String tail = content.substring(content.indexOf(" ") + 1, content.length());
			return tail;
		} else {
			return Properties.default_list_item;
		}
	}

	private static String first(Node node, ParseState ps) {
		final String content = evalTagContent(node, ps, null);
		return firstWord(content);

	}

	private static String rest(Node node, ParseState ps) {
		String content = evalTagContent(node, ps, null);
		content = ps.chatSession.bot.preProcessor.normalize(content);
		return restWords(content);

	}

	private static String resetlearnf(Node node, ParseState ps) {
		ps.chatSession.bot.deleteLearnfCategories();
		return "Deleted Learnf Categories";

	}

	private static String resetlearn(Node node, ParseState ps) {
		ps.chatSession.bot.deleteLearnCategories();
		return "Deleted Learn Categories";

	}

	/**
	 * Recursively descend the XML DOM tree, evaluating AIML and building a
	 * response.
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 */

	private static String recursEval(Node node, ParseState ps) {
		// Properties.trace("AIMLProcessor.recursEval(node: " + node + ", ps: " + ps
		// + ")");
		try {
			// Properties.trace("in AIMLProcessor.recursEval(), node string: " +
			// DomUtils.nodeToString(node));
			final String nodeName = node.getNodeName();
			// Properties.trace("in AIMLProcessor.recursEval(), nodeName: " + nodeName);
			// Properties.trace("in AIMLProcessor.recursEval(), node.getNodeValue(): " +
			// node.getNodeValue());
			if (nodeName.equals("#text")) {
				return node.getNodeValue();
			} else if (nodeName.equals("#comment")) {
				// Properties.trace("in AIMLProcessor.recursEval(), comment =
				// "+node.getTextContent());
				return "";
			} else if (nodeName.equals("template")) {
				return evalTagContent(node, ps, null);
			} else if (nodeName.equals("random")) {
				return random(node, ps);
			} else if (nodeName.equals("condition")) {
				return loopCondition(node, ps);
			} else if (nodeName.equals("srai")) {
				return srai(node, ps);
			} else if (nodeName.equals("sr")) {
				return respond(ps.starBindings.inputStars.star(0), ps.that, ps.topic, ps.chatSession, sraiCount);
			} else if (nodeName.equals("sraix")) {
				return sraix(node, ps);
			} else if (nodeName.equals("set")) {
				return set(node, ps);
			} else if (nodeName.equals("get")) {
				return get(node, ps);
			} else if (nodeName.equals("map")) {
				return map(node, ps);
			} else if (nodeName.equals("bot")) {
				return bot(node, ps);
			} else if (nodeName.equals("id")) {
				return id(node, ps);
			} else if (nodeName.equals("size")) {
				return size(node, ps);
			} else if (nodeName.equals("vocabulary")) {
				return vocabulary(node, ps);
			} else if (nodeName.equals("program")) {
				return program(node, ps);
			} else if (nodeName.equals("date")) {
				return date(node, ps);
			} else if (nodeName.equals("interval")) {
				return interval(node, ps);
			} else if (nodeName.equals("think")) {
				return think(node, ps);
			} else if (nodeName.equals("system")) {
				return system(node, ps);
			} else if (nodeName.equals("explode")) {
				return explode(node, ps);
			} else if (nodeName.equals("normalize")) {
				return normalize(node, ps);
			} else if (nodeName.equals("denormalize")) {
				return denormalize(node, ps);
			} else if (nodeName.equals("uppercase")) {
				return uppercase(node, ps);
			} else if (nodeName.equals("lowercase")) {
				return lowercase(node, ps);
			} else if (nodeName.equals("formal")) {
				return formal(node, ps);
			} else if (nodeName.equals("sentence")) {
				return sentence(node, ps);
			} else if (nodeName.equals("person")) {
				return person(node, ps);
			} else if (nodeName.equals("person2")) {
				return person2(node, ps);
			} else if (nodeName.equals("gender")) {
				return gender(node, ps);
			} else if (nodeName.equals("star")) {
				return inputStar(node, ps);
			} else if (nodeName.equals("thatstar")) {
				return thatStar(node, ps);
			} else if (nodeName.equals("topicstar")) {
				return topicStar(node, ps);
			} else if (nodeName.equals("that")) {
				return that(node, ps);
			} else if (nodeName.equals("input")) {
				return input(node, ps);
			} else if (nodeName.equals("request")) {
				return request(node, ps);
			} else if (nodeName.equals("response")) {
				return response(node, ps);
			} else if (nodeName.equals("learn") || nodeName.equals("learnf")) {
				return learn(node, ps);
			} else if (nodeName.equals("addtriple")) {
				return addTriple(node, ps);
			} else if (nodeName.equals("deletetriple")) {
				return deleteTriple(node, ps);
			} else if (nodeName.equals("javascript")) {
				return javascript(node, ps);
			} else if (nodeName.equals("select")) {
				return select(node, ps);
			} else if (nodeName.equals("uniq")) {
				return uniq(node, ps);
			} else if (nodeName.equals("first")) {
				return first(node, ps);
			} else if (nodeName.equals("rest")) {
				return rest(node, ps);
			} else if (nodeName.equals("resetlearnf")) {
				return resetlearnf(node, ps);
			} else if (nodeName.equals("resetlearn")) {
				return resetlearn(node, ps);
			} else if (extension != null && extension.extensionTagSet().contains(nodeName)) {
				return extension.recursEval(node, ps);
			} else {
				return (genericXML(node, ps));
			}
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
			return "";
		}
	}

	/**
	 * evaluate an AIML template expression
	 *
	 * @param template AIML template contents
	 * @param ps       AIML Parse state
	 * @return result of evaluating template.
	 */
	private static String evalTemplate(String template, ParseState ps) {
		// Properties.trace("AIMLProcessor.evalTemplate(template: " + template + ",
		// ps: " + ps + ")");
		String response = Properties.template_failed;
		try {
			template = "<template>" + template + "</template>";
			final Node root = DomUtils.parseString(template);
			response = recursEval(root, ps);
		} catch (final Exception e) {
			log.error(e.getMessage(), e);
		}
		// Properties.trace("in AIMLProcessor.evalTemplate() returning: " +
		// response);
		return response;
	}

}
