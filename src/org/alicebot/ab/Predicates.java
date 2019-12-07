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
	Library General Public License for more details.

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
import java.util.HashMap;

import org.alicebot.ab.utils.BotProperties;
import org.alicebot.ab.utils.JapaneseUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Manage client predicates
 *
 */
@Slf4j
public class Predicates extends HashMap<String, String> {
	private static final long serialVersionUID = 1L;

	/**
	 * save a predicate value
	 *
	 * @param key   predicate name
	 * @param value predicate value
	 * @return predicate value
	 */
	@Override
	public String put(String key, String value) {
		// MagicBooleans.trace("predicates.put(key: " + key + ", value: " + value +
		// ")");
		if (key.equals("topic")) {
			value = JapaneseUtils.tokenizeSentence(value);
		}

		if (key.equals("topic") && value.length() == 0) {
			value = BotProperties.default_get;
		}
		if (value.equals(BotProperties.too_much_recursion)) {
			value = BotProperties.default_list_item;
		}
		// MagicBooleans.trace("Setting predicate key: " + key + " to value: " + value);
		final String result = super.put(key, value);
		// MagicBooleans.trace("in predicates.put, returning: " + result);
		return result;
	}

	/**
	 * get a predicate value
	 *
	 * @param key predicate name
	 * @return predicate value
	 */
	public String get(String key) {
		// MagicBooleans.trace("predicates.get(key: " + key + ")");
		String result = super.get(key);
		if (result == null) {
			result = BotProperties.default_get;
		}
		// MagicBooleans.trace("in predicates.get, returning: " + result);
		return result;
	}

	/**
	 * Read predicate default values from an input stream
	 *
	 * @param in input stream
	 */
	public void getPredicateDefaultsFromInputStream(InputStream in) {
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		try {
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				if (strLine.contains(":")) {
					final String property = strLine.substring(0, strLine.indexOf(":"));
					final String value = strLine.substring(strLine.indexOf(":") + 1);
					put(property, value);
				}
			}
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}

	/**
	 * read predicate defaults from a file
	 *
	 * @param filename name of file
	 */
	public void getPredicateDefaults(String filename) {
		try {
			// Open the file that is the first
			// command line parameter
			final File file = new File(filename);
			if (file.exists()) {
				final FileInputStream fstream = new FileInputStream(filename);
				// Get the object
				getPredicateDefaultsFromInputStream(fstream);
				fstream.close();
			}
		} catch (final Exception e) {// Catch exception if any
			log.error("Error: " + e.getMessage());
		}
	}
}
