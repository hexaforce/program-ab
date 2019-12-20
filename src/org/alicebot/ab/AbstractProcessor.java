package org.alicebot.ab;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.alicebot.ab.utils.DomUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractProcessor {

	public static final AIMLProcessorExtension extension = null;

	static int sraiCount = 0;

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
	String recursEval(Node node, ParseState ps) {

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

	abstract String respond(String star, String that, String topic, Chat chatSession, int sraicount2);

	abstract String genericXML(Node node, ParseState ps);

	abstract String resetlearn(Node node, ParseState ps);

	abstract String resetlearnf(Node node, ParseState ps);

	abstract String rest(Node node, ParseState ps);

	abstract String first(Node node, ParseState ps);

	abstract String uniq(Node node, ParseState ps);

	abstract String select(Node node, ParseState ps);

	abstract String javascript(Node node, ParseState ps);

	abstract String deleteTriple(Node node, ParseState ps);

	abstract String addTriple(Node node, ParseState ps);

	abstract String learn(Node node, ParseState ps);

	abstract String response(Node node, ParseState ps);

	abstract String request(Node node, ParseState ps);

	abstract String input(Node node, ParseState ps);

	abstract String that(Node node, ParseState ps);

	abstract String topicStar(Node node, ParseState ps);

	abstract String thatStar(Node node, ParseState ps);

	abstract String inputStar(Node node, ParseState ps);

	abstract String gender(Node node, ParseState ps);

	abstract String person2(Node node, ParseState ps);

	abstract String person(Node node, ParseState ps);

	abstract String sentence(Node node, ParseState ps);

	abstract String formal(Node node, ParseState ps);

	abstract String lowercase(Node node, ParseState ps);

	abstract String uppercase(Node node, ParseState ps);

	abstract String denormalize(Node node, ParseState ps);

	abstract String normalize(Node node, ParseState ps);

	abstract String explode(Node node, ParseState ps);

	abstract String system(Node node, ParseState ps);

	abstract String think(Node node, ParseState ps);

	abstract String interval(Node node, ParseState ps);

	abstract String date(Node node, ParseState ps);

	abstract String program(Node node, ParseState ps);

	abstract String vocabulary(Node node, ParseState ps);

	abstract String size(Node node, ParseState ps);

	abstract String id(Node node, ParseState ps);

	abstract String bot(Node node, ParseState ps);

	abstract String map(Node node, ParseState ps);

	abstract String get(Node node, ParseState ps);

	abstract String set(Node node, ParseState ps);

	abstract String sraix(Node node, ParseState ps);

	abstract String srai(Node node, ParseState ps);

	abstract String loopCondition(Node node, ParseState ps);

	abstract String random(Node node, ParseState ps);

	abstract String evalTagContent(Node node, ParseState ps, Set<String> ignoreAttributes);

	String evalTemplate(String template, ParseState ps) {
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

	String unevaluatedXML(String resultIn, Node node, ParseState ps) {
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

	String estWords(String sentence) {
		String content = (sentence == null ? "" : sentence);
		content = content.trim();
		if (content.contains(" ")) {
			final String tail = content.substring(content.indexOf(" ") + 1, content.length());
			return tail;
		} else {
			return Properties.default_list_item;
		}
	}
	
	String restWords(String sentence) {
		String content = (sentence == null ? "" : sentence);
		content = content.trim();
		if (content.contains(" ")) {
			final String tail = content.substring(content.indexOf(" ") + 1, content.length());
			return tail;
		} else {
			return Properties.default_list_item;
		}
	}
	
	String firstWord(String sentence) {
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

	String tupleGet(String tupleName, String varName) {
		String result = Properties.default_get;
		final Tuple tuple = Tuple.tupleMap.get(tupleName);
		if (tuple == null) {
			result = Properties.default_get;
		} else {
			result = tuple.getValue(varName);
		}
		return result;
	}

	String getAttributeOrTagValue(Node node, ParseState ps, String attributeName) {
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

	String recursLearn(Node node, ParseState ps) {
		final String nodeName = node.getNodeName();
		if (nodeName.equals("#text")) {
			return node.getNodeValue();
		} else if (nodeName.equals("eval")) {
			return evalTagContent(node, ps, null);
		} else {
			return unevaluatedAIML(node, ps);
		}
	}

	String unevaluatedAIML(Node node, ParseState ps) {
		final String result = learnEvalTagContent(node, ps);
		return unevaluatedXML(result, node, ps);
	}

	String learnEvalTagContent(Node node, ParseState ps) {
		String result = "";
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			result += recursLearn(child, ps);
		}
		return result;
	}

	int getIndexValue(Node node, ParseState ps) {
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

	String capitalizeString(String string) {
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

	String explode(String input) {
		String result = "";
		for (int i = 0; i < input.length(); i++) {
			result += " " + input.charAt(i);
		}
		while (result.contains("  ")) {
			result = result.replace("  ", " ");
		}
		return result.trim();
	}

	String condition(Node node, ParseState ps) {

		// Make a list of all the <li> child nodes:
		final ArrayList<Node> liList = new ArrayList<Node>();
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			if ("li".equals(child.getNodeName())) {
				liList.add(child);
			}
		}

		String predicate = null, varName = null, value = null; // Node p=null, v=null;
		final HashSet<String> attributeNames = Utilities.stringSet("name", "var", "value");
		// First check if the <condition> has an attribute "name". If so, get the predicate name.
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
