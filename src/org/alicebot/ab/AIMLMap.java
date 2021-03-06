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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.alicebot.ab.utils.BotProperties;
import org.alicebot.ab.utils.Inflector;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * implements AIML Map
 *
 * A map is a function from one string set to another. Elements of the domain
 * are called keys and elements of the range are called values.
 *
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = false)
public class AIMLMap extends HashMap<String, String> {
	private static final long serialVersionUID = 1L;

	private String mapName;
	private String host; // for external maps
	private String botid; // for external maps
	private boolean isExternal = false;
	private Inflector inflector = Inflector.getInstance();
	private Bot bot;

	/**
	 * constructor to create a new AIML Map
	 *
	 * @param name the name of the map
	 * @param bot  bot
	 */
	public AIMLMap(String name, Bot bot) {
		super();
		this.bot = bot;
		this.mapName = name;
	}

	/**
	 * return a map value given a key
	 *
	 * @param key the domain element
	 * @return the range element or a string indicating the key was not found
	 */
	public String get(String key) {
		String value;
		if (mapName.equals(BotProperties.map_successor)) {
			try {
				final int number = Integer.parseInt(key);
				return String.valueOf(number + 1);
			} catch (final Exception ex) {
				return BotProperties.default_map;
			}
		} else if (mapName.equals(BotProperties.map_predecessor)) {
			try {
				final int number = Integer.parseInt(key);
				return String.valueOf(number - 1);
			} catch (final Exception ex) {
				return BotProperties.default_map;
			}
		} else if (mapName.equals("singular")) {
			return inflector.singularize(key).toLowerCase();
		} else if (mapName.equals("plural")) {
			return inflector.pluralize(key).toLowerCase();
		} else if (isExternal && BotProperties.enable_external_sets) {
			// String[] split = key.split(" ");
			final String query = mapName.toUpperCase() + " " + key;
			final String response = Sraix.sraix(null, query, BotProperties.default_map, null, host, botid, null, "0");
			log.info("External " + mapName + "(" + key + ")=" + response);
			value = response;
		} else {
			value = super.get(key);
		}
		if (value == null) {
			value = BotProperties.default_map;
		}
		// log.info("AIMLMap get "+key+"="+value);
		return value;
	}

	/**
	 * put a new key, value pair into the map.
	 *
	 * @param key   the domain element
	 * @param value the range element
	 * @return the value
	 */
	@Override
	public String put(String key, String value) {
		// log.info("AIMLMap put "+key+"="+value);
		return super.put(key, value);
	}

	public void writeAIMLMap() {
		log.info("Writing AIML Map " + mapName);
		try {
			// Create file
			final FileWriter fstream = new FileWriter(bot.maps_path + "/" + mapName + ".txt");
			final BufferedWriter out = new BufferedWriter(fstream);
			for (String p : this.keySet()) {
				p = p.trim();
				// log.info(p+"-->"+this.get(p));
				out.write(p + ":" + this.get(p).trim());
				out.newLine();
			}
			// Close the output stream
			out.close();
		} catch (final Exception e) {// Catch exception if any
			log.error("Error: " + e.getMessage());
		}
	}

	public int readAIMLMapFromInputStream(InputStream in, Bot bot) {
		int cnt = 0;
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		// Read File Line By Line
		try {
			while ((strLine = br.readLine()) != null && strLine.length() > 0) {
				final String[] splitLine = strLine.split(":");
				// log.info("AIMLMap line="+strLine);
				if (splitLine.length >= 2) {
					cnt++;
					if (strLine.startsWith(BotProperties.remote_map_key)) {
						if (splitLine.length >= 3) {
							host = splitLine[1];
							botid = splitLine[2];
							isExternal = true;
							log.info("Created external map at " + host + " " + botid);
						}
					} else {
						final String key = splitLine[0].toUpperCase();
						final String value = splitLine[1];
						// assume domain element is already normalized for speedier load
						// key = bot.preProcessor.normalize(key).trim();
						put(key, value);
					}
				}
			}
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		return cnt;
	}

	/**
	 * read an AIML map for a bot
	 *
	 * @param bot the bot associated with this map.
	 * @return count
	 */
	public int readAIMLMap(Bot bot) {
		int cnt = 0;

		final File file = new File(bot.maps_path + "/" + mapName + ".txt");
		if (!file.exists()) {
			log.info(file.getPath() + " not found");
			return cnt;
		}
		log.debug("Reading AIML Map " + file.getPath());

		try {
			// Open the file that is the first
			// command line parameter
			final FileInputStream fstream = new FileInputStream(file);
			// Get the object
			cnt = readAIMLMapFromInputStream(fstream, bot);
			fstream.close();
		} catch (final Exception e) {
			// Catch exception if any
			log.error("Error: " + e.getMessage());
		}
		return cnt;

	}

}
