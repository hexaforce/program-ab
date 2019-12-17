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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * implements AIML Sets
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
public
class AIMLSet extends HashSet<String> {
	private static final long serialVersionUID = 1L;

	private String setName;
	private int maxLength = 1; // there are no empty sets
	private String host; // for external sets
	private String botid; // for external sets
	private boolean isExternal = false;
	private Bot bot;
	private final HashSet<String> inCache = new HashSet<String>();
	private final HashSet<String> outCache = new HashSet<String>();

	/**
	 * constructor
	 * 
	 * @param name name of set
	 * @param bot  bot
	 */
	public AIMLSet(String name, Bot bot) {
		super();
		this.bot = bot;
		this.setName = name.toLowerCase();
		if (setName.equals(Properties.natural_number_set_name)) {
			maxLength = 1;
		}
	}

	boolean contains(String s) {
		// if (isExternal) log.info("External "+setName+" contains "+s+"?");
		// else log.info("Internal "+setName+" contains "+s+"?");
		if (isExternal && Properties.enable_external_sets) {
			if (inCache.contains(s)) {
				return true;
			}
			if (outCache.contains(s)) {
				return false;
			}
			final String[] split = s.split(" ");
			if (split.length > maxLength) {
				return false;
			}
			final String query = Properties.set_member_string + setName.toUpperCase() + " " + s;
			final String response = Sraix.sraix(null, query, "false", null, host, botid, null, "0");
			// log.info("External "+setName+" contains "+s+"? "+response);
			if (response.equals("true")) {
				inCache.add(s);
				return true;
			} else {
				outCache.add(s);
				return false;
			}
		} else if (setName.equals(Properties.natural_number_set_name)) {
			final Pattern numberPattern = Pattern.compile("[0-9]+");
			final Matcher numberMatcher = numberPattern.matcher(s);
			final Boolean isanumber = numberMatcher.matches();
			// log.info("AIMLSet isanumber '"+s+"' "+isanumber);
			return isanumber;
		} else {
			return super.contains(s);
		}
	}

	int readAIMLSetFromInputStream(InputStream in, Bot bot) {
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		int cnt = 0;
		// Read File Line By Line
		try {
			while ((strLine = br.readLine()) != null && strLine.length() > 0) {
				cnt++;
				// strLine = bot.preProcessor.normalize(strLine).toUpperCase();
				// assume the set is pre-normalized for faster loading
				if (strLine.startsWith("external")) {
					final String[] splitLine = strLine.split(":");
					if (splitLine.length >= 4) {
						host = splitLine[1];
						botid = splitLine[2];
						maxLength = Integer.parseInt(splitLine[3]);
						isExternal = true;
						log.info("Created external set at " + host + " " + botid);
					}
				} else {
					strLine = strLine.toUpperCase().trim();
					final String[] splitLine = strLine.split(" ");
					final int length = splitLine.length;
					if (length > maxLength) {
						maxLength = length;
					}
					// log.info("readAIMLSetFromInputStream "+strLine);
					add(strLine.trim());
				}
				/*
				 * Category c = new Category(0,
				 * "ISA"+setName.toUpperCase()+" "+strLine.toUpperCase(), "*", "*", "true",
				 * Properties.null_aiml_file); bot.brain.addCategory(c);
				 */
			}
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return cnt;
	}

	int readAIMLSet(Bot bot) {
		int cnt = 0;

		final File file = new File(bot.sets_path + "/" + setName + ".txt");
		if (!file.exists()) {
			log.info(file.getPath() + " not found");
			return cnt;
		}
		log.debug("Reading AIML Set " + file.getPath());

		try {
			// Open the file that is the first
			// command line parameter
			final FileInputStream fstream = new FileInputStream(file);
			// Get the object
			cnt = readAIMLSetFromInputStream(fstream, bot);
			fstream.close();
		} catch (final Exception e) {// Catch exception if any
			log.error("Error: " + e.getMessage());
		}
		return cnt;

	}

}
