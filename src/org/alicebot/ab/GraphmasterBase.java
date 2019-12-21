package org.alicebot.ab;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GraphmasterBase {

	protected final Bot bot;
	protected final Nodemapper root;
	private HashSet<String> vocabulary;

	public GraphmasterBase(Bot bot) {
		this.bot = bot;
		this.root = new Nodemapper();
		this.vocabulary = new HashSet<String>();
	}

	private boolean ENABLE_SHORT_CUTS = true;

	private String botPropRegex = "<bot name=\"(.*?)\"/>";
	private Pattern botPropPattern = Pattern.compile(botPropRegex, Pattern.CASE_INSENSITIVE);

	private String replaceBotProperties(String pattern) {
		if (pattern.contains("<B")) {
			final Matcher matcher = botPropPattern.matcher(pattern);
			while (matcher.find()) {
				final String propname = matcher.group(1).toLowerCase();
				final String property = bot.properties.get(propname).toUpperCase();
				pattern = pattern.replaceFirst("(?i)" + botPropRegex, property);
			}
		}
		return pattern;
	}

	void addCategory(Category category) {
		String inputThatTopic = inputThatTopic(category.getPattern(), category.getThat(), category.getTopic());
		addPath(root, Path.sentenceToPath(replaceBotProperties(inputThatTopic)), category);
	}

	static String inputThatTopic(String input, String that, String topic) {
		return input.trim() + " <THAT> " + that.trim() + " <TOPIC> " + topic.trim();
	}

	private void addPath(Nodemapper node, Path path, Category category) {
		if (path == null) {
			node.category = category;
			node.height = 0;
		} else if (ENABLE_SHORT_CUTS && thatStarTopicStar(path)) {
			node.category = category;
			node.height = Math.min(4, node.height);
			node.shortCut = true;
		} else if (NodemapperOperator.containsKey(node, path.word)) {
			if (path.word.startsWith("<SET>")) {
				addSets(path.word, bot, node, category.getFilename());
			}
			final Nodemapper nextNode = NodemapperOperator.get(node, path.word);
			addPath(nextNode, path.next, category);
			int offset = 1;
			if (path.word.equals("#") || path.word.equals("^")) {
				offset = 0;
			}
			node.height = Math.min(offset + nextNode.height, node.height);
		} else {
			final Nodemapper nextNode = new Nodemapper();
			if (path.word.startsWith("<SET>")) {
				addSets(path.word, bot, node, category.getFilename());
			}
			if (node.key != null) {
				NodemapperOperator.upgrade(node);
			}
			NodemapperOperator.put(node, path.word, nextNode);
			addPath(nextNode, path.next, category);
			int offset = 1;
			if (path.word.equals("#") || path.word.equals("^")) {
				offset = 0;
			}
			node.height = Math.min(offset + nextNode.height, node.height);
		}
	}

	private boolean thatStarTopicStar(Path path) {
		final String tail = Path.pathToSentence(path).trim();
		return tail.equals("<THAT> * <TOPIC> *");
	}

	private void addSets(String type, Bot bot, Nodemapper node, String filename) {
		final String setName = Utilities.tagTrim(type, "SET").toLowerCase();
		if (bot.setMap.containsKey(setName)) {
			if (node.sets == null) {
				node.sets = new ArrayList<String>();
			} else if (!node.sets.contains(setName)) {
				node.sets.add(setName);
			}
		} else {
			log.info("No AIML Set found for <set>" + setName + "</set> in " + bot.botName + " " + filename);
		}
	}

	Nodemapper findNode(Category c) {
		return findNode(root, Path.sentenceToPath(inputThatTopic(c.getPattern(), c.getThat(), c.getTopic())));
	}

	private Nodemapper findNode(Nodemapper node, Path path) {
		if (path == null && node != null) {
			return node;
		} else if (Path.pathToSentence(path).trim().equals("<THAT> * <TOPIC> *") && node.shortCut && path.word.equals("<THAT>")) {
			return node;
		} else if (NodemapperOperator.containsKey(node, path.word)) {
			final Nodemapper nextNode = NodemapperOperator.get(node, path.word.toUpperCase());
			return findNode(nextNode, path.next);
		} else {
			return null;
		}
	}

	void printgraph() {
		printgraph(root, "");
	}

	private void printgraph(Nodemapper node, String partial) {
		if (node == null) {
			log.info("Null graph");
			return;
		}
		String template = "";
		if (NodemapperOperator.isLeaf(node) || node.shortCut) {
			template = Category.templateToLine(node.category.getTemplate());
			template = template.substring(0, Math.min(16, template.length()));
			if (node.shortCut) {
				log.info(partial + "(" + NodemapperOperator.size(node) + "[" + node.height + "])--<THAT>-->X(1)--*-->X(1)--<TOPIC>-->X(1)--*-->" + template + "...");
			} else {
				log.info(partial + "(" + NodemapperOperator.size(node) + "[" + node.height + "]) " + template + "...");
			}
		}
		for (final String key : NodemapperOperator.keySet(node)) {
			printgraph(NodemapperOperator.get(node, key), partial + "(" + NodemapperOperator.size(node) + "[" + node.height + "])--" + key + "-->");
		}
	}

	int getCategoriesSize() {
		return getCategories().size();
	}

	ArrayList<Category> getCategories() {
		final ArrayList<Category> categories = new ArrayList<Category>();
		getCategories(root, categories);
		return categories;
	}

	private void getCategories(Nodemapper node, ArrayList<Category> categories) {
		if (node == null) {
			return;
		}
		if (node.category != null && (NodemapperOperator.isLeaf(node) || node.shortCut)) {
			categories.add(node.category);
		}
		for (final String key : NodemapperOperator.keySet(node)) {
			getCategories(NodemapperOperator.get(node, key), categories);
		}
	}

	void nodeStats() {
		nodeStatsGraph(root);
	}

	private void nodeStatsGraph(Nodemapper node) {
		if (node != null) {
			for (final String key : NodemapperOperator.keySet(node)) {
				nodeStatsGraph(NodemapperOperator.get(node, key));
			}
		}
	}

	HashSet<String> getVocabulary() {
		vocabulary = new HashSet<String>();
		getBrainVocabulary(root);
		for (final String set : bot.setMap.keySet()) {
			vocabulary.addAll(bot.setMap.get(set));
		}
		return vocabulary;
	}

	private void getBrainVocabulary(Nodemapper node) {
		if (node != null) {
			for (final String key : NodemapperOperator.keySet(node)) {
				vocabulary.add(key);
				getBrainVocabulary(NodemapperOperator.get(node, key));
			}
		}
	}

}
