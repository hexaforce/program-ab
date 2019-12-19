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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * AIML Preprocessor and substitutions
 */
@Slf4j
class PreProcessor {

	enum Substitution {
		normal, denormal, person, person2, gender;
		File file(String dir) {
			return new File(dir + "/" + name() + ".txt");
		}
	}
	final private Bot bot;
	final private Map<String, List<String>> substitutions = new HashMap<String, List<String>>();
	final private Map<String, List<Pattern>> patterns= new HashMap<String, List<Pattern>>();
	
	/**
	 * Constructor given bot
	 *
	 * @param bot AIML bot
	 */
	PreProcessor(Bot bot) {
		this.bot = bot;
		for (Substitution s :Substitution.values()) {
			readSubstitutions(s);
		}
	}
	
	/**
	 * apply normalization substitutions to a request
	 *
	 * @param request client input
	 * @return normalized client input
	 */
	String normalize(String request) {
		String result = substitute(request, Substitution.normal);
		return result.replaceAll("(\r\n|\n\r|\r|\n)", " ");
	}

	/**
	 * apply denormalization substitutions to a request
	 *
	 * @param request client input
	 * @return normalized client input
	 */
	String denormalize(String request) {
		return substitute(request, Substitution.denormal);
	}

	/**
	 * personal pronoun substitution for {@code <person></person>} tag
	 * 
	 * @param input sentence
	 * @return sentence with pronouns swapped
	 */
	String person(String request) {
		return substitute(request, Substitution.person);
	}

	/**
	 * personal pronoun substitution for {@code <person2></person2>} tag
	 * 
	 * @param input sentence
	 * @return sentence with pronouns swapped
	 */
	String person2(String request) {
		return substitute(request, Substitution.person2);
	}

	/**
	 * personal pronoun substitution for {@code <gender>} tag
	 * 
	 * @param input sentence
	 * @return sentence with pronouns swapped
	 */
	String gender(String request) {
		return substitute(request, Substitution.gender);
	}

	/**
	 * Apply a sequence of subsitutions to an input string
	 *
	 * @param request  input request
	 * @param patterns array of patterns to match
	 * @param subs     array of substitution values
	 * @param count    number of patterns and substitutions
	 * @return result of applying substitutions to input
	 */
	private String substitute(String request, Substitution key) {
		String result = " " + request + " ";
		List<String> sub = substitutions.get(key.name());
		List<Pattern> pat = patterns.get(key.name());
		for (int i = 0; i < sub.size(); i++) {
			final String replacement = sub.get(i);
			final Pattern p = pat.get(i);
			final Matcher m = p.matcher(result);
			if (m.find()) {
				result = m.replaceAll(replacement);
			}
		}
		while (result.contains("  ")) {
			result = result.replace("  ", " ");
		}
		return result.trim();
	}

	/**
	 * read substitutions from a file
	 *
	 * @param filename name of substitution file
	 * @param patterns array of patterns
	 * @param subs     array of substitution values
	 * @return number of patterns and substitutions read
	 */
	private void readSubstitutions(Substitution key) {

		final File file = key.file(bot.config_path);
		if (!file.exists()) {
			return;
		}
		
		try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			final List<String> substitutionList = new ArrayList<String>();
			final List<Pattern> patternList = new ArrayList<Pattern>();
			String strLine;
			while ((strLine = br.readLine()) != null) {
				strLine = strLine.trim();
				if (!strLine.startsWith(Properties.text_comment_mark)) {
					final Pattern pattern = Pattern.compile("\"(.*?)\",\"(.*?)\"", Pattern.DOTALL);
					final Matcher matcher = pattern.matcher(strLine);
					if (matcher.find()) {
						substitutionList.add(matcher.group(2));
						final String quotedPattern = Pattern.quote(matcher.group(1));
						patternList.add(Pattern.compile(quotedPattern, Pattern.CASE_INSENSITIVE));
					}
				}
			}
			substitutions.put(key.name(), substitutionList);
			patterns.put(key.name(), patternList);
		} catch (final Exception e) {
			log.error("Error: " + e.getMessage());
		}
		
	}

	/**
	 * Split an input into an array of sentences based on sentence-splitting
	 * characters.
	 *
	 * @param line input text
	 * @return array of sentences
	 */
	String[] sentenceSplit(String line) {
		line = line.replace("。", ".");
		line = line.replace("？", "?");
		line = line.replace("！", "!");
		final String result[] = line.split("[\\.!\\?]");
		for (int i = 0; i < result.length; i++) {
			result[i] = result[i].trim();
		}
		return result;
	}

}
