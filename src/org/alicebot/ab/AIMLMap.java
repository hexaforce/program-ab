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
import java.io.InputStreamReader;
import java.util.HashMap;

import org.alicebot.ab.utils.Inflector;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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

	public final String mapName;
	private String host; // for external maps
	private String botid; // for external maps
	private boolean isExternal = false;
	private Inflector inflector = Inflector.getInstance();
	private Bot bot;

	@Getter
	private int cnt = 0;

	enum BuiltInMap {
		successor, predecessor, plural, singular;
		File file(String dir) {
			return new File(dir + "/" + name() + ".txt");
		}
	}

	/**
	 * constructor to create a new AIML Map
	 *
	 * @param name the name of the map
	 * @param bot  bot
	 */
	public AIMLMap(File file, Bot bot) {
		super();
		this.bot = bot;
		this.mapName = file.getName().substring(0, file.getName().length() - ".txt".length());

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			String strLine;
			while ((strLine = reader.readLine()) != null && strLine.length() > 0) {
				final String[] splitLine = strLine.split(":");
				if (splitLine.length >= 2) {
					cnt++;
					if (strLine.startsWith(Properties.remote_map_key)) {
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
		} catch (final Exception e) {
			log.error("Error: " + e.getMessage());
		}

	}

	/**
	 * return a map value given a key
	 *
	 * @param key the domain element
	 * @return the range element or a string indicating the key was not found
	 */
	String get(String key) {
		String value;
		if (mapName.equals(Properties.map_successor)) {
			try {
				final int number = Integer.parseInt(key);
				return String.valueOf(number + 1);
			} catch (final Exception ex) {
				return Properties.default_map;
			}
		} else if (mapName.equals(Properties.map_predecessor)) {
			try {
				final int number = Integer.parseInt(key);
				return String.valueOf(number - 1);
			} catch (final Exception ex) {
				return Properties.default_map;
			}
		} else if (mapName.equals("singular")) {
			return inflector.singularize(key).toLowerCase();
		} else if (mapName.equals("plural")) {
			return inflector.pluralize(key).toLowerCase();
		} else if (isExternal && Properties.enable_external_sets) {
			// String[] split = key.split(" ");
			final String query = mapName.toUpperCase() + " " + key;
			final String response = Sraix.sraix(null, query, Properties.default_map, null, host, botid, null, "0");
			log.info("External " + mapName + "(" + key + ")=" + response);
			value = response;
		} else {
			value = super.get(key);
		}
		if (value == null) {
			value = Properties.default_map;
		}
		// log.info("AIMLMap get "+key+"="+value);
		return value;
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

}
