package org.alicebot.ab;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.alicebot.ab.utils.CalendarUtils;
import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.IntervalUtils;
import org.alicebot.ab.utils.JapaneseUtils;
import org.w3c.dom.Node;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AIMLProcessor2 extends AbstractProcessor {

	final static String NL = System.getProperty("line.separator");

	@Override
	public String respond(String input, String that, String topic, Chat chatSession, int srCnt) {
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

	@Override
	public String genericXML(Node node, ParseState ps) {
		final String evalResult = evalTagContent(node, ps, null);
		final String result = unevaluatedXML(evalResult, node, ps);
		return result;
	}

	@Override
	public String resetlearn(Node node, ParseState ps) {
		ps.chatSession.bot.deleteLearnfCategories();
		return "Deleted Learnf Categories";
	}

	@Override
	public String resetlearnf(Node node, ParseState ps) {
		ps.chatSession.bot.deleteLearnCategories();
		return "Deleted Learn Categories";
	}

	@Override
	public String rest(Node node, ParseState ps) {
		String content = evalTagContent(node, ps, null);
		content = ps.chatSession.bot.preProcessor.normalize(content);
		return restWords(content);
	}

	@Override
	public String first(Node node, ParseState ps) {
		final String content = evalTagContent(node, ps, null);
		return firstWord(content);
	}

	@Override
	public String uniq(Node node, ParseState ps) {

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

	@Override
	public String select(Node node, ParseState ps) {

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

	@Override
	public String javascript(Node node, ParseState ps) {
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

	@Override
	public String deleteTriple(Node node, ParseState ps) {
		final String subject = getAttributeOrTagValue(node, ps, "subj");
		final String predicate = getAttributeOrTagValue(node, ps, "pred");
		final String object = getAttributeOrTagValue(node, ps, "obj");
		return ps.chatSession.tripleStore.deleteTriple(subject, predicate, object);
	}

	@Override
	public String addTriple(Node node, ParseState ps) {
		final String subject = getAttributeOrTagValue(node, ps, "subj");
		final String predicate = getAttributeOrTagValue(node, ps, "pred");
		final String object = getAttributeOrTagValue(node, ps, "obj");
		return ps.chatSession.tripleStore.addTriple(subject, predicate, object);
	}

	@Override
	public String learn(Node node, ParseState ps) {

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

	@Override
	public String response(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		return ps.chatSession.responseHistory.getString(index).trim();
	}

	@Override
	public String request(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		return ps.chatSession.requestHistory.getString(index).trim();
	}

	@Override
	public String input(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		return ps.chatSession.inputHistory.getString(index);
	}

	@Override
	public String that(Node node, ParseState ps) {
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

	@Override
	public String topicStar(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		if (ps.starBindings.topicStars.star(index) == null) {
			return "";
		} else {
			return ps.starBindings.topicStars.star(index).trim();
		}
	}

	@Override
	public String thatStar(Node node, ParseState ps) {
		final int index = getIndexValue(node, ps);
		if (ps.starBindings.thatStars.star(index) == null) {
			return "";
		} else {
			return ps.starBindings.thatStars.star(index).trim();
		}
	}

	@Override
	public String inputStar(Node node, ParseState ps) {
		String result = "";
		final int index = getIndexValue(node, ps);
		if (ps.starBindings.inputStars.star(index) == null) {
			result = "";
		} else {
			result = ps.starBindings.inputStars.star(index).trim();
		}
		return result;
	}

	@Override
	public String gender(Node node, ParseState ps) {
		String result = evalTagContent(node, ps, null);
		result = " " + result + " ";
		result = ps.chatSession.bot.preProcessor.gender(result);
		return result.trim();
	}

	@Override
	public String person2(Node node, ParseState ps) {
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

	@Override
	public String person(Node node, ParseState ps) {
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

	@Override
	public String sentence(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		if (result.length() > 1) {
			return result.substring(0, 1).toUpperCase() + result.substring(1, result.length());
		} else {
			return "";
		}
	}

	@Override
	public String formal(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return capitalizeString(result);
	}

	@Override
	public String lowercase(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return result.toLowerCase();
	}

	@Override
	public String uppercase(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return result.toUpperCase();
	}

	@Override
	public String denormalize(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return ps.chatSession.bot.preProcessor.denormalize(result);
	}

	@Override
	public String normalize(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		final String returning = ps.chatSession.bot.preProcessor.normalize(result);
		return returning;
	}

	@Override
	public String explode(Node node, ParseState ps) {
		final String result = evalTagContent(node, ps, null);
		return explode(result);
	}

	@Override
	public String system(Node node, ParseState ps) {
		final HashSet<String> attributeNames = Utilities.stringSet("timeout");
		final String evaluatedContents = evalTagContent(node, ps, attributeNames);
		final String result = IOUtils.system(evaluatedContents, Properties.system_failed);
		return result;
	}

	@Override
	public String think(Node node, ParseState ps) {
		evalTagContent(node, ps, null);
		return "";
	}

	@Override
	public String interval(Node node, ParseState ps) {
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

	@Override
	public String date(Node node, ParseState ps) {
		final String jformat = getAttributeOrTagValue(node, ps, "jformat");
		final String locale = getAttributeOrTagValue(node, ps, "locale");
		final String timezone = getAttributeOrTagValue(node, ps, "timezone");
		return CalendarUtils.date(jformat, locale, timezone);
	}

	@Override
	public String program(Node node, ParseState ps) {
		return Properties.program_name_version;
	}

	@Override
	public String vocabulary(Node node, ParseState ps) {
		final int size = ps.chatSession.bot.brain.getVocabulary().size();
		return String.valueOf(size);
	}

	@Override
	public String size(Node node, ParseState ps) {
		final int size = ps.chatSession.bot.brain.getCategoriesSize();
		return String.valueOf(size);
	}

	@Override
	public String id(Node node, ParseState ps) {
		return ps.chatSession.customerId;
	}

	@Override
	public String bot(Node node, ParseState ps) {
		String result = Properties.default_property;
		final String propertyName = getAttributeOrTagValue(node, ps, "name");
		if (propertyName != null) {
			result = ps.chatSession.bot.properties.get(propertyName).trim();
		}
		return result;
	}

	@Override
	public String map(Node node, ParseState ps) {
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

	@Override
	public String get(Node node, ParseState ps) {
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

	@Override
	public String set(Node node, ParseState ps) {
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

	@Override
	public String sraix(Node node, ParseState ps) {
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

	@Override
	public String srai(Node node, ParseState ps) {
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

	@Override
	public String loopCondition(Node node, ParseState ps) {
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

	@Override
	public String random(Node node, ParseState ps) {
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

	@Override
	public String evalTagContent(Node node, ParseState ps, Set<String> ignoreAttributes) {
		String result = "";
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			if (ignoreAttributes == null || !ignoreAttributes.contains(child.getNodeName())) {
				result += recursEval(child, ps);
			}
		}
		return result;
	}

}
