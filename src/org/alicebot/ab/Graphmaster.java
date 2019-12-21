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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * The AIML Pattern matching algorithm and data structure.
 */
@Slf4j
class Graphmaster {

	private final String botPropRegex = "<bot name=\"(.*?)\"/>";
	private Pattern botPropPattern = Pattern.compile(botPropRegex, Pattern.CASE_INSENSITIVE);

	private final String replaceBotProperties(String pattern) {
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

//	void printgraph() {
//		printgraph(root, "");
//	}
//
//	private void printgraph(Nodemapper node, String partial) {
//		if (node == null) {
//			log.info("Null graph");
//			return;
//		}
//		String template = "";
//		if (NodemapperOperator.isLeaf(node) || node.shortCut) {
//			template = Category.templateToLine(node.category.getTemplate());
//			template = template.substring(0, Math.min(16, template.length()));
//			if (node.shortCut) {
//				log.info(partial + "(" + NodemapperOperator.size(node) + "[" + node.height + "])--<THAT>-->X(1)--*-->X(1)--<TOPIC>-->X(1)--*-->" + template + "...");
//			} else {
//				log.info(partial + "(" + NodemapperOperator.size(node) + "[" + node.height + "]) " + template + "...");
//			}
//		}
//		for (final String key : NodemapperOperator.keySet(node)) {
//			printgraph(NodemapperOperator.get(node, key), partial + "(" + NodemapperOperator.size(node) + "[" + node.height + "])--" + key + "-->");
//		}
//	}

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

	public Graphmaster(Bot bot) {
		this.bot = bot;
		this.root = new Nodemapper();
		this.vocabulary = new HashSet<String>();
	}

	protected final Bot bot;
	protected final Nodemapper root;
	private HashSet<String> vocabulary;

	private boolean ENABLE_SHORT_CUTS = true;

	private final int MAX_STARS = 1000;

	final Nodemapper match(String input, String that, String topic) {

		final String inputThatTopic = inputThatTopic(input, that, topic);
		final Path path = Path.sentenceToPath(inputThatTopic);

		try {
			final String[] inputStars = new String[MAX_STARS];
			final String[] thatStars = new String[MAX_STARS];
			final String[] topicStars = new String[MAX_STARS];
			final String starState = "inputStar";
			final String matchTrace = "";
			final Nodemapper mapper = match(path, root, inputThatTopic, starState, 0, inputStars, thatStars, topicStars, matchTrace);
			if (mapper != null) {
				final StarBindings bind = new StarBindings();
				for (int i = 0; inputStars[i] != null && i < MAX_STARS; i++) {
					bind.inputStars.add(inputStars[i]);
				}
				for (int i = 0; thatStars[i] != null && i < MAX_STARS; i++) {
					bind.thatStars.add(thatStars[i]);
				}
				for (int i = 0; topicStars[i] != null && i < MAX_STARS; i++) {
					bind.topicStars.add(topicStars[i]);
				}
				mapper.starBindings = bind;
			}
			if (mapper != null) {
				mapper.category.addMatch(inputThatTopic, bot);
			}
			return mapper;
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
			return null;
		}
	}

	private final Nodemapper match(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		Nodemapper matchedNode;
		if ((matchedNode = nullMatch(path, node, matchTrace)) != null) {
			return matchedNode;
		} else if (path.length < node.height) {
			return null;
		} else if ((matchedNode = dollarMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
			return matchedNode;
		} else if ((matchedNode = sharpMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
			return matchedNode;
		} else if ((matchedNode = underMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
			return matchedNode;
		} else if ((matchedNode = wordMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
			return matchedNode;
		} else if ((matchedNode = setMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
			return matchedNode;
		} else if ((matchedNode = shortCutMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
			return matchedNode;
		} else if ((matchedNode = caretMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
			return matchedNode;
		} else if ((matchedNode = starMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
			return matchedNode;
		} else {
			return null;
		}
	}

	private final Nodemapper nullMatch(Path path, Nodemapper node, String matchTrace) {
		if (path == null && node != null && NodemapperOperator.isLeaf(node) && node.category != null) {
			return node;
		} else {
			fail("null", matchTrace);
			return null;
		}
	}

	private final Nodemapper shortCutMatch(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		if (node != null && node.shortCut && path.word.equals("<THAT>") && node.category != null) {
			final String tail = Path.pathToSentence(path).trim();
			thatStars[0] = tail.substring(tail.indexOf("<THAT>") + "<THAT>".length(), tail.indexOf("<TOPIC>")).trim();
			topicStars[0] = tail.substring(tail.indexOf("<TOPIC>") + "<TOPIC>".length(), tail.length()).trim();
			return node;
		} else {
			fail("shortCut", matchTrace);
			return null;
		}
	}

	private final Nodemapper wordMatch(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		Nodemapper matchedNode;
		try {
			final String uword = path.word.toUpperCase();
			if (uword.equals("<THAT>")) {
				starIndex = 0;
				starState = "thatStar";
			} else if (uword.equals("<TOPIC>")) {
				starIndex = 0;
				starState = "topicStar";
			}
			matchTrace += "[" + uword + "," + uword + "]";
			if (path != null && NodemapperOperator.containsKey(node, uword) && (matchedNode = match(path.next, NodemapperOperator.get(node, uword), inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
				return matchedNode;
			} else {
				fail("word", matchTrace);
				return null;
			}
		} catch (final Exception ex) {
			log.info("wordMatch: " + Path.pathToSentence(path) + ": " + ex);
			log.error(ex.getMessage(), ex);
			return null;
		}
	}

	private final Nodemapper dollarMatch(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		final String uword = "$" + path.word.toUpperCase();
		Nodemapper matchedNode;
		if (path != null && NodemapperOperator.containsKey(node, uword) && (matchedNode = match(path.next, NodemapperOperator.get(node, uword), inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
			return matchedNode;
		} else {
			fail("dollar", matchTrace);
			return null;
		}
	}

	private final Nodemapper starMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "*", matchTrace);
	}

	private final Nodemapper underMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "_", matchTrace);
	}

	private final Nodemapper caretMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		Nodemapper matchedNode = zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^", matchTrace);
		if (matchedNode != null) {
			return matchedNode;
		} else {
			return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^", matchTrace);
		}
	}

	private final Nodemapper sharpMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		Nodemapper matchedNode = zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "#", matchTrace);
		if (matchedNode != null) {
			return matchedNode;
		} else {
			return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "#", matchTrace);
		}
	}

	private final Nodemapper zeroMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String wildcard, String matchTrace) {
		matchTrace += "[" + wildcard + ",]";
		if (path != null && NodemapperOperator.containsKey(node, wildcard)) {
			setStars(bot.properties.get(Properties.null_star), starIndex, starState, inputStars, thatStars, topicStars);
			final Nodemapper nextNode = NodemapperOperator.get(node, wildcard);
			return match(path, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace);
		} else {
			fail("zero " + wildcard, matchTrace);
			return null;
		}

	}

	private final Nodemapper wildMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String wildcard, String matchTrace) {
		if (path.word.equals("<THAT>") || path.word.equals("<TOPIC>")) {
			fail("wild1 " + wildcard, matchTrace);
			return null;
		}
		Nodemapper matchedNode;
		try {
			if (path != null && NodemapperOperator.containsKey(node, wildcard)) {
				matchTrace += "[" + wildcard + "," + path.word + "]";
				String currentWord = path.word;
				String starWords = currentWord + " ";
				Path pathStart = path.next;
				final Nodemapper nextNode = NodemapperOperator.get(node, wildcard);
				if (NodemapperOperator.isLeaf(nextNode) && !nextNode.shortCut) {
					matchedNode = nextNode;
					starWords = Path.pathToSentence(path);
					setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
					return matchedNode;
				} else {
					for (path = pathStart; path != null && !currentWord.equals("<THAT>") && !currentWord.equals("<TOPIC>"); path = path.next) {
						matchTrace += "[" + wildcard + "," + path.word + "]";
						if ((matchedNode = match(path, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace)) != null) {
							setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
							return matchedNode;
						} else {
							currentWord = path.word;
							starWords += currentWord + " ";
						}
					}
					fail("wild2 " + wildcard, matchTrace);
					return null;
				}
			}
		} catch (final Exception ex) {
			log.info("wildMatch: " + Path.pathToSentence(path) + ": " + ex);
		}
		fail("wild3 " + wildcard, matchTrace);
		return null;
	}

	private final Nodemapper setMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		// log.debug("Graphmaster.setMatch(path: " + path + ", node: " + node + ",
		// input: " + input + ", starState: " + starState + ", starIndex: " + starIndex
		// + ", inputStars, thatStars, topicStars, matchTrace: " + matchTrace + ", )");
		if (node.sets == null || path.word.equals("<THAT>") || path.word.equals("<TOPIC>")) {
			return null;
		}
		// log.debug("in Graphmaster.setMatch, setMatch sets =" + node.sets);
		for (final String setName : node.sets) {
			// log.debug("in Graphmaster.setMatch, setMatch trying type " + setName);
			final Nodemapper nextNode = NodemapperOperator.get(node, "<SET>" + setName.toUpperCase() + "</SET>");
			final AIMLSet aimlSet = bot.setMap.get(setName);
			Nodemapper matchedNode;
			Nodemapper bestMatchedNode = null;
			String currentWord = path.word;
			String starWords = currentWord + " ";
			int length = 1;
			matchTrace += "[<set>" + setName + "</set>," + path.word + "]";
			// log.debug("in Graphmaster.setMatch, setMatch starWords =\"" + starWords +
			// "\"");
			for (Path qath = path.next; qath != null && !currentWord.equals("<THAT>") && !currentWord.equals("<TOPIC>") && length <= aimlSet.getMaxLength(); qath = qath.next) {
				// log.debug("in Graphmaster.setMatch, qath.word = " + qath.word);
				final String phrase = bot.preProcessor.normalize(starWords.trim()).toUpperCase();
				// log.debug("in Graphmaster.setMatch, setMatch trying \"" + phrase + "\" in " +
				// setName);
				if (aimlSet.contains(phrase) && (matchedNode = match(qath, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace)) != null) {
					setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
					// log.debug("in Graphmaster.setMatch, setMatch found " + phrase + " in " +
					// setName);
					bestMatchedNode = matchedNode;
				}
				length = length + 1;
				currentWord = qath.word;
				starWords += currentWord + " ";
			}
			if (bestMatchedNode != null) {
				return bestMatchedNode;
			}
		}
		fail("set", matchTrace);
		return null;
	}

	private void fail(String mode, String trace) {
		// log.info("Match failed (" + mode + ") " + trace);
	}

	private void setStars(String starWords, int starIndex, String starState, String[] inputStars, String[] thatStars, String[] topicStars) {
		if (starIndex < Properties.max_stars) {
			starWords = starWords.trim();
			if (starState.equals("inputStar")) {
				inputStars[starIndex] = starWords;
			} else if (starState.equals("thatStar")) {
				thatStars[starIndex] = starWords;
			} else if (starState.equals("topicStar")) {
				topicStars[starIndex] = starWords;
			}
		}
	}

}
