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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * AIML Preprocessor and substitutions
 */
@Slf4j
class PreProcessor {

	int normalCount = 0;
	int denormalCount = 0;
	int personCount = 0;
	int person2Count = 0;
	int genderCount = 0;
	String[] normalSubs = new String[Properties.max_substitutions];
	Pattern[] normalPatterns = new Pattern[Properties.max_substitutions];
	String[] denormalSubs = new String[Properties.max_substitutions];
	Pattern[] denormalPatterns = new Pattern[Properties.max_substitutions];
	String[] personSubs = new String[Properties.max_substitutions];
	Pattern[] personPatterns = new Pattern[Properties.max_substitutions];
	String[] person2Subs = new String[Properties.max_substitutions];
	Pattern[] person2Patterns = new Pattern[Properties.max_substitutions];
	String[] genderSubs = new String[Properties.max_substitutions];
	Pattern[] genderPatterns = new Pattern[Properties.max_substitutions];

	/**
	 * Constructor given bot
	 *
	 * @param bot AIML bot
	 */
	PreProcessor(Bot bot) {

		normalCount = readSubstitutions(bot.config_path + "/normal.txt", normalPatterns, normalSubs);
		denormalCount = readSubstitutions(bot.config_path + "/denormal.txt", denormalPatterns, denormalSubs);
		personCount = readSubstitutions(bot.config_path + "/person.txt", personPatterns, personSubs);
		person2Count = readSubstitutions(bot.config_path + "/person2.txt", person2Patterns, person2Subs);
		genderCount = readSubstitutions(bot.config_path + "/gender.txt", genderPatterns, genderSubs);

		log.debug("Preprocessor: " + normalCount + " norms " + personCount + " persons " + person2Count + " person2 ");
	}

	/**
	 * apply normalization substitutions to a request
	 *
	 * @param request client input
	 * @return normalized client input
	 */
	String normalize(String request) {
		log.debug("PreProcessor.normalize(request: " + request + ")");
		String result = substitute(request, normalPatterns, normalSubs, normalCount);
		result = result.replaceAll("(\r\n|\n\r|\r|\n)", " ");
		log.debug("PreProcessor.normalize() returning: " + result);
		return result;
	}

	/**
	 * apply denormalization substitutions to a request
	 *
	 * @param request client input
	 * @return normalized client input
	 */
	String denormalize(String request) {
		return substitute(request, denormalPatterns, denormalSubs, denormalCount);
	}

	/**
	 * personal pronoun substitution for {@code <person></person>} tag
	 * 
	 * @param input sentence
	 * @return sentence with pronouns swapped
	 */
	String person(String input) {
		return substitute(input, personPatterns, personSubs, personCount);

	}

	/**
	 * personal pronoun substitution for {@code <person2></person2>} tag
	 * 
	 * @param input sentence
	 * @return sentence with pronouns swapped
	 */
	String person2(String input) {
		return substitute(input, person2Patterns, person2Subs, person2Count);

	}

	/**
	 * personal pronoun substitution for {@code <gender>} tag
	 * 
	 * @param input sentence
	 * @return sentence with pronouns swapped
	 */
	String gender(String input) {
		return substitute(input, genderPatterns, genderSubs, genderCount);

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
	String substitute(String request, Pattern[] patterns, String[] subs, int count) {
		String result = " " + request + " ";
		int index = 0;
		try {
			for (int i = 0; i < count; i++) {
				index = i;
				final String replacement = subs[i];
				final Pattern p = patterns[i];
				final Matcher m = p.matcher(result);
				// log.info(i+" "+patterns[i].pattern()+"-->"+subs[i]);
				if (m.find()) {
					// log.info(i+" "+patterns[i].pattern()+"-->"+subs[i]);
					// log.info(m.group());
					result = m.replaceAll(replacement);
				}

				// log.info(result);
			}
			while (result.contains("  ")) {
				result = result.replace("  ", " ");
			}
			result = result.trim();
			// log.info("Normalized: "+result);
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
			log.info("Request " + request + " Result " + result + " at " + index + " " + patterns[index] + " " + subs[index]);
		}
		return result.trim();
	}

	/**
	 * read substitutions from input stream
	 *
	 * @param in       input stream
	 * @param patterns array of patterns
	 * @param subs     array of substitution values
	 * @return number of patterns substitutions read
	 */
	int readSubstitutionsFromInputStream(InputStream in, Pattern[] patterns, String[] subs) {
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		// Read File Line By Line
		int subCount = 0;
		try {
			while ((strLine = br.readLine()) != null) {
				// log.info(strLine);
				strLine = strLine.trim();
				if (!strLine.startsWith(Properties.text_comment_mark)) {
					final Pattern pattern = Pattern.compile("\"(.*?)\",\"(.*?)\"", Pattern.DOTALL);
					final Matcher matcher = pattern.matcher(strLine);
					if (matcher.find() && subCount < Properties.max_substitutions) {
						subs[subCount] = matcher.group(2);
						final String quotedPattern = Pattern.quote(matcher.group(1));
						// log.info("quoted pattern="+quotedPattern);
						patterns[subCount] = Pattern.compile(quotedPattern, Pattern.CASE_INSENSITIVE);
						subCount++;
					}
				}

			}
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return subCount;
	}

	/**
	 * read substitutions from a file
	 *
	 * @param filename name of substitution file
	 * @param patterns array of patterns
	 * @param subs     array of substitution values
	 * @return number of patterns and substitutions read
	 */
	int readSubstitutions(String filename, Pattern[] patterns, String[] subs) {
		int subCount = 0;
		try {

			// Open the file that is the first
			// command line parameter
			final File file = new File(filename);
			if (file.exists()) {
				final FileInputStream fstream = new FileInputStream(filename);
				// Get the object of DataInputStream
				subCount = readSubstitutionsFromInputStream(fstream, patterns, subs);
				// Close the input stream
				fstream.close();
			}
		} catch (final Exception e) {// Catch exception if any
			log.error("Error: " + e.getMessage());
		}
		return (subCount);
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
		// log.info("Sentence split "+line);
		final String result[] = line.split("[\\.!\\?]");
		for (int i = 0; i < result.length; i++) {
			result[i] = result[i].trim();
		}
		return result;
	}

	/**
	 * normalize a file consisting of sentences, one sentence per line.
	 *
	 * @param infile  input file
	 * @param outfile output file to write results
	 */
	void normalizeFile(String infile, String outfile) {
		try {
			BufferedWriter bw = null;
			final FileInputStream fstream = new FileInputStream(infile);
			final BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			bw = new BufferedWriter(new FileWriter(outfile));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				strLine = strLine.trim();
				if (strLine.length() > 0) {
					final String norm = normalize(strLine).toUpperCase();
					final String sentences[] = sentenceSplit(norm);
					{
						if (sentences.length > 1) {
							for (final String s : sentences) {
								log.info(norm + "-->" + s);
							}
						}
						for (String sentence : sentences) {
							sentence = sentence.trim();
							if (sentence.length() > 0) {
								// log.info("'"+strLine+"'-->'"+norm+"'");
								bw.write(sentence);
								bw.newLine();
							}
						}
					}
				}
			}
			bw.close();
			br.close();
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}
}
