package org.alicebot.ab;

import java.util.ArrayList;

import org.alicebot.ab.utils.DomUtils;
import org.alicebot.ab.utils.JapaneseUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AIMLToCategories {
	
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
	public static ArrayList<Category> toCategories(String aimlFileName, Node node) {
		final ArrayList<Category> categories = new ArrayList<Category>();
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			if ("category".equals(child.getNodeName())) {
				categories.add(categoryProcessor(aimlFileName, child));
			} else if ("topic".equals(child.getNodeName())) {
				final String topic = child.getAttributes().getNamedItem("name").getTextContent();
				for (Node grandchild : new IterableNodeList(child.getChildNodes())) {
					categories.add(categoryProcessor(aimlFileName, grandchild, topic));
				}
			}
		}
		return categories;
	}

	private static Category categoryProcessor(String aimlFile, Node node) {
		return categoryProcessor(aimlFile, node, "*");
	}

	/**
	 * when parsing an AIML file, process a category element.
	 *
	 * @param n          current XML parse node.
	 * @param categories list of categories found so far.
	 * @param topic      value of topic in case this category is wrapped in a
	 *                   {@code <topic>} tag
	 * @param aimlFile   name of AIML file being parsed.
	 */
	private static Category categoryProcessor(String aimlFile, Node node, String topic) {
		String pattern, that, template;

		pattern = "*";
		that = "*";
		template = "";

		for (Node child : new IterableNodeList(node.getChildNodes())) {
			if ("#text".equals(child.getNodeName())) {
				continue;
			}
			String nodeToString = DomUtils.nodeToString(child);
			if ("pattern".equals(child.getNodeName())) {
				nodeToString = trimTag(nodeToString, "pattern");
				nodeToString = cleanPattern(nodeToString);
				pattern = JapaneseUtils.tokenizeSentence(nodeToString);
			} else if ("that".equals(child.getNodeName())) {
				nodeToString = trimTag(nodeToString, "that");
				nodeToString = cleanPattern(nodeToString);
				that = JapaneseUtils.tokenizeSentence(nodeToString);
			} else if ("topic".equals(child.getNodeName())) {
				nodeToString = trimTag(nodeToString, "topic");
				nodeToString = cleanPattern(nodeToString);
				topic = JapaneseUtils.tokenizeSentence(nodeToString);
			} else if ("template".equals(child.getNodeName())) {
				template = nodeToString;
			} else {
				log.warn("categoryProcessor: unexpected " + child.getNodeName() + " in " + DomUtils.nodeToString(child));
			}
		}

		final Category c = new Category(0, pattern, that, topic, template, aimlFile);
		if (StringUtils.isBlank(template)) {
			log.error("Category " + c.inputThatTopic() + " discarded due to blank or missing <template>.");
		}
		return c;
	}

}
