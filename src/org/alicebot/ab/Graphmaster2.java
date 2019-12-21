package org.alicebot.ab;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Graphmaster2 extends GraphmasterBase{

	public Graphmaster2(Bot bot) {
		super(bot);
	}
	
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
			final String that = tail.substring(tail.indexOf("<THAT>") + "<THAT>".length(), tail.indexOf("<TOPIC>")).trim();
			final String topic = tail.substring(tail.indexOf("<TOPIC>") + "<TOPIC>".length(), tail.length()).trim();
			thatStars[0] = that;
			topicStars[0] = topic;
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
		Nodemapper matchedNode;
		matchedNode = zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^", matchTrace);
		if (matchedNode != null) {
			return matchedNode;
		} else {
			return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^", matchTrace);
		}
	}

	private final Nodemapper sharpMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
		Nodemapper matchedNode;
		matchedNode = zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "#", matchTrace);
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
		Nodemapper matchedNode;
		if (path.word.equals("<THAT>") || path.word.equals("<TOPIC>")) {
			fail("wild1 " + wildcard, matchTrace);
			return null;
		}
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
		//log.debug("Graphmaster.setMatch(path: " + path + ", node: " + node + ", input: " + input + ", starState: " + starState + ", starIndex: " + starIndex + ", inputStars, thatStars, topicStars, matchTrace: " + matchTrace + ", )");
		if (node.sets == null || path.word.equals("<THAT>") || path.word.equals("<TOPIC>")) {
			return null;
		}
		//log.debug("in Graphmaster.setMatch, setMatch sets =" + node.sets);
		for (final String setName : node.sets) {
			//log.debug("in Graphmaster.setMatch, setMatch trying type " + setName);
			final Nodemapper nextNode = NodemapperOperator.get(node, "<SET>" + setName.toUpperCase() + "</SET>");
			final AIMLSet aimlSet = bot.setMap.get(setName);
			Nodemapper matchedNode;
			Nodemapper bestMatchedNode = null;
			String currentWord = path.word;
			String starWords = currentWord + " ";
			int length = 1;
			matchTrace += "[<set>" + setName + "</set>," + path.word + "]";
			//log.debug("in Graphmaster.setMatch, setMatch starWords =\"" + starWords + "\"");
			for (Path qath = path.next; qath != null && !currentWord.equals("<THAT>") && !currentWord.equals("<TOPIC>") && length <= aimlSet.getMaxLength(); qath = qath.next) {
				//log.debug("in Graphmaster.setMatch, qath.word = " + qath.word);
				final String phrase = bot.preProcessor.normalize(starWords.trim()).toUpperCase();
				//log.debug("in Graphmaster.setMatch, setMatch trying \"" + phrase + "\" in " + setName);
				if (aimlSet.contains(phrase) && (matchedNode = match(qath, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace)) != null) {
					setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
					//log.debug("in Graphmaster.setMatch, setMatch found " + phrase + " in " + setName);
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
		log.info("Match failed (" + mode + ") " + trace);
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
