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

import lombok.extern.slf4j.Slf4j;

/**
 * The core AIML parser and interpreter. Implements the AIML 2.1 specification
 * as described in AIML 2.1 Working Draft document
 * https://docs.google.com/document/d/1wNT25hJRyupcG51aO89UcQEiG-HkXRXusukADpFnDs4/pub
 */
@Slf4j
public class AIMLProcessor {

	public static AIMLProcessorExtension extension = null;

	static int sraiCount = 0;

	static String respond(String input, String that, String topic, Chat chatSession, int srCnt) {
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
			final String template = leaf.category.getTemplate();

			response = evalTemplate(template, ps);

		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}

		return response;
	}

	static String genericXML(Node node, ParseState ps) {
		final String evalResult = evalTagContent(node, ps, null);
		final String result = unevaluatedXML(evalResult, node, ps);
		return result;
	}

	private static String resetlearn(Node node, ParseState ps) {
		ps.chatSession.bot.deleteLearnfCategories();
		return "Deleted Learnf Categories";
	}

	private static String resetlearnf(Node node, ParseState ps) {
		ps.chatSession.bot.deleteLearnCategories();
		return "Deleted Learn Categories";
	}

	private static String rest(Node node, ParseState ps) {
		String content = evalTagContent(node, ps, null);
		content = ps.chatSession.bot.preProcessor.normalize(content);
		return restWords(content);
	}

	private static String first(Node node, ParseState ps) {
		final String content = evalTagContent(node, ps, null);
		return firstWord(content);
	}

	private static String uniq(Node node, ParseState ps) {

		final HashSet<String> vars = new HashSet<String>();
		final HashSet<String> visibleVars = new HashSet<String>();

		String subj = "?subject";
		String pred = "?predicate";
		String obj = "?object";

		for (Node child : new IterableNodeList(node.getChildNodes())) {
			final String contents = evalTagContent(child, ps, null);
			if ("subj".equals(child.getNodeName())) {
				subj = contents;
			} else if ("pred".equals(child.getNodeName())) {
				pred = contents;
			} else if ("obj".equals(child.getNodeName())) {
				obj = contents;
			}
			if (contents.startsWith("?")) {
				visibleVars.add(contents);
				vars.add(contents);
			}
		}

		final Tuple partial = new Tuple(vars, visibleVars, null);
		final Clause clause = Clause.builder().subj(subj).pred(pred).obj(obj).affirm(true).build();
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
		final HashSet<String> vars = new HashSet<String>();
		final HashSet<String> visibleVars = new HashSet<String>();

		for (Node child : new IterableNodeList(node.getChildNodes())) {
			if ("vars".equals(child.getNodeName())) {

				final String contents = evalTagContent(child, ps, null);
				final String[] splitVars = contents.split(" ");
				for (String var : splitVars) {
					var = var.trim();
					if (var.length() > 0) {
						visibleVars.add(var);
					}
				}

			} else if ("q".equals(child.getNodeName()) || "notq".equals(child.getNodeName())) {

				final Boolean affirm = !"notq".equals(child.getNodeName());
				String subj = null;
				String pred = null;
				String obj = null;

				for (Node grandchild : new IterableNodeList(child.getChildNodes())) {
					final String contents = evalTagContent(grandchild, ps, null);
					if ("subj".equals(grandchild.getNodeName())) {
						subj = contents;
					} else if ("pred".equals(grandchild.getNodeName())) {
						pred = contents;
					} else if ("obj".equals(grandchild.getNodeName())) {
						obj = contents;
					}
					if (contents.startsWith("?")) {
						vars.add(contents);
					}
				}

				final Clause clause = Clause.builder().subj(subj).pred(pred).obj(obj).affirm(affirm).build();
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

	private static String learn(Node node, ParseState ps) {
		final String NL = System.getProperty("line.separator");

		String pattern = "";
		String that = "*";
		String template = "";

		for (Node child : new IterableNodeList(node.getChildNodes())) {
			if ("category".equals(child.getNodeName())) {

				for (Node grandchild : new IterableNodeList(child.getChildNodes())) {
					if ("pattern".equals(grandchild.getNodeName())) {
						pattern = recursLearn(grandchild, ps);
					} else if ("that".equals(grandchild.getNodeName())) {
						that = recursLearn(grandchild, ps);
					} else if ("template".equals(grandchild.getNodeName())) {
						template = recursLearn(grandchild, ps);
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
					c = new Category(0, pattern, that, "*", template, Properties.null_aiml_file);
					ps.chatSession.bot.learnGraph.addCategory(c);
				} else {
					// learnf
					c = new Category(0, pattern, that, "*", template, Properties.learnf_aiml_file);
					ps.chatSession.bot.learnfGraph.addCategory(c);
				}
				ps.chatSession.bot.brain.addCategory(c);
			}
		}
		return "";
	}

	private static String response(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		return ps.chatSession.responseHistory.getString(index).trim();
	}

	private static String request(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		return ps.chatSession.requestHistory.getString(index).trim();
	}

	private static String input(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		return ps.chatSession.inputHistory.getString(index);
	}

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

	private static String topicStar(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		if (ps.starBindings.topicStars.star(index) == null) {
			return "";
		} else {
			return ps.starBindings.topicStars.star(index).trim();
		}
	}

	private static String thatStar(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		if (ps.starBindings.thatStars.star(index) == null) {
			return "";
		} else {
			return ps.starBindings.thatStars.star(index).trim();
		}
	}

	private static String inputStar(Node node, ParseState ps) {
		String result = "";
		final int index = getIndexValue(node, ps);
		if (ps.starBindings.inputStars.star(index) == null) {
			result = "";
		} else {
			result = ps.starBindings.inputStars.star(index).trim();
		}
		return result;
	}

	private static String gender(Node node, ParseState ps) {
		String result = evalTagContent(node, ps, null);
		result = " " + result + " ";
		result = ps.chatSession.bot.preProcessor.gender(result);
		return result.trim();
	}

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

	private static String sentence(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		if (result.length() > 1) {
			return result.substring(0, 1).toUpperCase() + result.substring(1, result.length());
		} else {
			return "";
		}
	}

	private static String formal(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return capitalizeString(result);
	}

	private static String lowercase(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return result.toLowerCase();
	}

	private static String uppercase(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return result.toUpperCase();
	}

	private static String denormalize(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return ps.chatSession.bot.preProcessor.denormalize(result);
	}

	private static String normalize(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		final String returning = ps.chatSession.bot.preProcessor.normalize(result);
		return returning;
	}

	private static String explode(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return explode(result);
	}

	private static String system(Node node, ParseState ps) {
		final HashSet<String> attributeNames = Utilities.stringSet("timeout");
		final String evaluatedContents = evalTagContent(node, ps, attributeNames);
		final String result = IOUtils.system(evaluatedContents, Properties.system_failed);
		return result;
	}

	private static String think(Node node, ParseState ps) {
		evalTagContent(node, ps, null);
		return "";
	}

	private static String interval(Node node, ParseState ps) {
		String style = getAttributeOrTagValue(node, ps, "style");
		String jformat = getAttributeOrTagValue(node, ps, "jformat");
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

	private static String date(Node node, ParseState ps) {
		final String jformat = getAttributeOrTagValue(node, ps, "jformat");
		final String locale = getAttributeOrTagValue(node, ps, "locale");
		final String timezone = getAttributeOrTagValue(node, ps, "timezone");
		return CalendarUtils.date(jformat, locale, timezone);
	}

	private static String program(Node node, ParseState ps) {
		return Properties.program_name_version;
	}

	private static String vocabulary(Node node, ParseState ps) {
		final int size = ps.chatSession.bot.brain.getVocabulary().size();
		return String.valueOf(size);
	}

	private static String size(Node node, ParseState ps) {
		final int size = ps.chatSession.bot.brain.getCategoriesSize();
		return String.valueOf(size);
	}

	private static String id(Node node, ParseState ps) {
		return ps.chatSession.customerId;
	}

	private static String bot(Node node, ParseState ps) {
		String result = Properties.default_property;
		final String propertyName = getAttributeOrTagValue(node, ps, "name");
		if (propertyName != null) {
			result = ps.chatSession.bot.properties.get(propertyName).trim();
		}
		return result;
	}

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
			if (result == null) {
				result = Properties.default_map;
			}
			result = result.trim();
		}
		return result;
	}

	private static String get(Node node, ParseState ps) {
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
		return result;
	}

	private static String set(Node node, ParseState ps) {
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
		return result;
	}

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

	private static String srai(Node node, ParseState ps) {
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
			final String topic = ps.chatSession.predicates.get("topic"); // the that stays the same, but the topic may have changed
			final Nodemapper leaf = ps.chatSession.bot.brain.match(result, ps.that, topic);
			if (leaf == null) {
				return (response);
			}
			response = evalTemplate(leaf.category.getTemplate(), new ParseState(ps.depth + 1, ps.chatSession, ps.input, ps.that, topic, leaf, new Predicates(), leaf.starBindings));
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return response.trim();
	}

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

	private static String random(Node node, ParseState ps) {
		final ArrayList<Node> liList = new ArrayList<Node>();
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			if ("li".equals(child.getNodeName())) {
				liList.add(child);
			}
		}
		int index = (int) (Math.random() * liList.size());
		if (Properties.qa_test_mode) {
			index = 0;
		}
		return evalTagContent(liList.get(index), ps, null);
	}

	static String evalTagContent(Node node, ParseState ps, Set<String> ignoreAttributes) {
		String result = "";
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			if (ignoreAttributes == null || !ignoreAttributes.contains(child.getNodeName())) {
				result += recursEval(child, ps);
			}
		}
		return result;
	}

	enum Tag {
		template, random, condition, srai, sr, sraix, set, get, map, bot, id, size, vocabulary, program, date, interval, think, system, explode, normalize, denormalize, uppercase, lowercase, formal, sentence, person, person2, gender, star, thatstar, topicstar, that, input, request, response, learn, learnf, addtriple, deletetriple, javascript, select, uniq, first, rest, resetlearnf, resetlearn;
	}

	/**
	 * Recursively descend the XML DOM tree, evaluating AIML and building a
	 * response.
	 *
	 * @param node current XML parse node
	 * @param ps   AIML parse state
	 */
	private static String recursEval(Node node, ParseState ps) {

		final String nodeName = node.getNodeName();
		if (nodeName.equals("#text")) {
			return node.getNodeValue();
		} else if (nodeName.equals("#comment")) {
			return "";
		}

		try {
			switch (Tag.valueOf(nodeName)) {
			case addtriple:
				return addTriple(node, ps);
			case bot:
				return bot(node, ps);
			case condition:
				return loopCondition(node, ps);
			case date:
				return date(node, ps);
			case deletetriple:
				return deleteTriple(node, ps);
			case denormalize:
				return denormalize(node, ps);
			case explode:
				return explode(node, ps);
			case first:
				return first(node, ps);
			case formal:
				return formal(node, ps);
			case gender:
				return gender(node, ps);
			case get:
				return get(node, ps);
			case id:
				return id(node, ps);
			case input:
				return input(node, ps);
			case interval:
				return interval(node, ps);
			case javascript:
				return javascript(node, ps);
			case learn:
			case learnf:
				return learn(node, ps);
			case lowercase:
				return lowercase(node, ps);
			case map:
				return map(node, ps);
			case normalize:
				return normalize(node, ps);
			case person:
				return person(node, ps);
			case person2:
				return person2(node, ps);
			case program:
				return program(node, ps);
			case random:
				return random(node, ps);
			case request:
				return request(node, ps);
			case resetlearn:
				return resetlearn(node, ps);
			case resetlearnf:
				return resetlearnf(node, ps);
			case response:
				return response(node, ps);
			case rest:
				return rest(node, ps);
			case select:
				return select(node, ps);
			case sentence:
				return sentence(node, ps);
			case set:
				return set(node, ps);
			case size:
				return size(node, ps);
			case sr:
				return respond(ps.starBindings.inputStars.star(0), ps.that, ps.topic, ps.chatSession, sraiCount);
			case srai:
				return srai(node, ps);
			case sraix:
				return sraix(node, ps);
			case star:
				return inputStar(node, ps);
			case system:
				return system(node, ps);
			case template:
				return evalTagContent(node, ps, null);
			case that:
				return that(node, ps);
			case thatstar:
				return thatStar(node, ps);
			case think:
				return think(node, ps);
			case topicstar:
				return topicStar(node, ps);
			case uniq:
				return uniq(node, ps);
			case uppercase:
				return uppercase(node, ps);
			case vocabulary:
				return vocabulary(node, ps);
			default:
				break;
			}
		} catch (IllegalArgumentException e) {
			// no match tag.
		}
		if (extension != null && extension.extensionTagSet().contains(nodeName)) {
			return extension.recursEval(node, ps);
		}
		return genericXML(node, ps);
	}

	private static String evalTemplate(String template, ParseState ps) {
		String response = Properties.template_failed;
		try {
			template = "<template>" + template + "</template>";
			final Node root = DomUtils.parseString(template);
			response = recursEval(root, ps);
		} catch (final Exception e) {
			log.error(e.getMessage(), e);
		}
		return response;
	}

	private static String unevaluatedXML(String resultIn, Node node, ParseState ps) {
		final String nodeName = node.getNodeName();
		String attributes = "";
		if (node.hasAttributes()) {
			final NamedNodeMap XMLAttributes = node.getAttributes();
			for (int i = 0; i < XMLAttributes.getLength(); i++) {
				attributes += " " + XMLAttributes.item(i).getNodeName() + "=\"" + XMLAttributes.item(i).getNodeValue() + "\"";
			}
		}
		String result = "<" + nodeName + attributes + "/>";
		if (!resultIn.equals("")) {
			result = "<" + nodeName + attributes + ">" + resultIn + "</" + nodeName + ">";
		}
		return result;
	}

//	private static String estWords(String sentence) {
//		String content = (sentence == null ? "" : sentence);
//		content = content.trim();
//		if (content.contains(" ")) {
//			final String tail = content.substring(content.indexOf(" ") + 1, content.length());
//			return tail;
//		} else {
//			return Properties.default_list_item;
//		}
//	}

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

	private static String tupleGet(String tupleName, String varName) {
		String result = Properties.default_get;
		final Tuple tuple = Tuple.tupleMap.get(tupleName);
		if (tuple == null) {
			result = Properties.default_get;
		} else {
			result = tuple.getValue(varName);
		}
		return result;
	}

	private static String getAttributeOrTagValue(Node node, ParseState ps, String attributeName) {
		String result = "";
		final Node m = node.getAttributes().getNamedItem(attributeName);
		if (m == null) {
			result = null;
			for (Node child : new IterableNodeList(node.getChildNodes())) {
				if (child.getNodeName().equals(attributeName)) {
					result = evalTagContent(child, ps, null);
				}
			}
		} else {
			result = m.getNodeValue();
		}
		return result;
	}

	private static String recursLearn(Node node, ParseState ps) {
		final String nodeName = node.getNodeName();
		if (nodeName.equals("#text")) {
			return node.getNodeValue();
		} else if (nodeName.equals("eval")) {
			return evalTagContent(node, ps, null);
		} else {
			return unevaluatedAIML(node, ps);
		}
	}

	private static String unevaluatedAIML(Node node, ParseState ps) {
		final String result = learnEvalTagContent(node, ps);
		return unevaluatedXML(result, node, ps);
	}

	private static String learnEvalTagContent(Node node, ParseState ps) {
		String result = "";
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			result += recursLearn(child, ps);
		}
		return result;
	}

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

	private static String condition(Node node, ParseState ps) {

		// Make a list of all the <li> child nodes:
		final ArrayList<Node> liList = new ArrayList<Node>();
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			if ("li".equals(child.getNodeName())) {
				liList.add(child);
			}
		}

		String predicate = null, varName = null, value = null; // Node p=null, v=null;
		final HashSet<String> attributeNames = Utilities.stringSet("name", "var", "value");
		// First check if the <condition> has an attribute "name". If so, get the
		// predicate name.
		predicate = getAttributeOrTagValue(node, ps, "name");
		varName = getAttributeOrTagValue(node, ps, "var");

		// if there are no <li> nodes, this is a one-shot condition.
		if (liList.size() == 0 && (value = getAttributeOrTagValue(node, ps, "value")) != null && predicate != null && ps.chatSession.predicates.get(predicate).equalsIgnoreCase(value)) {
			return evalTagContent(node, ps, attributeNames);
		} else if (liList.size() == 0 && (value = getAttributeOrTagValue(node, ps, "value")) != null && varName != null && ps.vars.get(varName).equalsIgnoreCase(value)) {
			return evalTagContent(node, ps, attributeNames);
		} else {
			// otherwise this is a <condition> with <li> items:
			for (Node n : liList) {
				String liPredicate = predicate;
				String liVarName = varName;
				if (liPredicate == null) {
					liPredicate = getAttributeOrTagValue(n, ps, "name");
				}
				if (liVarName == null) {
					liVarName = getAttributeOrTagValue(n, ps, "var");
				}
				value = getAttributeOrTagValue(n, ps, "value");
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
}
