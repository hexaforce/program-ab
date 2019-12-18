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
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * Bot Properties
 */
@Slf4j
public class Properties extends HashMap<String, String> {
	private static final long serialVersionUID = 1L;

	// General global strings
	public static String program_name_version = "Program AB 0.0.6.26 beta -- AI Foundation Reference AIML 2.1 implementation";
//	public static String comment = "Added repetition detection.";
//	public static String aimlif_split_char = ",";
	public static String default_bot = "alice2";
	public static String default_language = "EN";
	public static String aimlif_split_char_name = "\\#Comma";
	public static String aimlif_file_suffix = ".csv";
	public static String ab_sample_file = "sample.txt";
	public static String text_comment_mark = ";;";
	// <sraix> defaults
	public static String pannous_api_key = "guest";
	public static String pannous_login = "test-user";
	public static String sraix_failed = "SRAIXFAILED";
	public static String repetition_detected = "REPETITIONDETECTED";
	public static String sraix_no_hint = "nohint";
	public static String sraix_event_hint = "event";
	public static String sraix_pic_hint = "pic";
	public static String sraix_shopping_hint = "shopping";
	// AIML files
	public static String unknown_aiml_file = "unknown_aiml_file.aiml";
	public static String deleted_aiml_file = "deleted.aiml";
	public static String learnf_aiml_file = "learnf.aiml";
	public static String null_aiml_file = "null.aiml";
	public static String inappropriate_aiml_file = "inappropriate.aiml";
	public static String profanity_aiml_file = "profanity.aiml";
	public static String insult_aiml_file = "insults.aiml";
	public static String reductions_update_aiml_file = "reductions_update.aiml";
	public static String predicates_aiml_file = "client_profile.aiml";
	public static String update_aiml_file = "update.aiml";
	public static String personality_aiml_file = "personality.aiml";
	public static String sraix_aiml_file = "sraix.aiml";
	public static String oob_aiml_file = "oob.aiml";
//	public static String unfinished_aiml_file = "unfinished.aiml";
	// filter responses
	public static String inappropriate_filter = "FILTER INAPPROPRIATE";
	public static String profanity_filter = "FILTER PROFANITY";
	public static String insult_filter = "FILTER INSULT";
	// default templates
	public static String deleted_template = "deleted";
//	public static String unfinished_template = "unfinished";
	// AIML defaults
	public static String bad_javascript = "JSFAILED";
	public static String js_enabled = "true";
	public static String unknown_history_item = "unknown";
	public static String default_bot_response = "会話を理解・認識できません。";
	public static String error_bot_response = "Something is wrong with my brain.";
	public static String schedule_error = "I'm unable to schedule that event.";
	public static String system_failed = "Failed to execute system command.";
	public static String default_get = "unknown";
	public static String default_property = "unknown";
	public static String default_map = "unknown";
	public static String default_Customer_id = "unknown";
//	public static String default_bot_name = "unknown";
	public static String default_that = "unknown";
	public static String default_topic = "unknown";
	public static String default_list_item = "NIL";
	public static String undefined_triple = "NIL";
	public static String unbound_variable = "unknown";
	public static String template_failed = "Template failed.";
	public static String too_much_recursion = "Too much recursion in AIML";
	public static String too_much_looping = "Too much looping in AIML";
	public static String blank_template = "blank template";
	public static String null_input = "NORESP";
	public static String null_star = "nullstar";
	// sets and maps
	public static String set_member_string = "ISA";
	public static String remote_map_key = "external";
//	public static String remote_set_key = "external";
	public static String natural_number_set_name = "number";
	public static String map_successor = "successor";
	public static String map_predecessor = "predecessor";
	public static String map_singular = "singular";
	public static String map_plural = "plural";

	//
	public static int node_activation_cnt = 4; // minimum number of activations to suggest atomic pattern
	public static int node_size = 4; // minimum number of branches to suggest wildcard pattern
	public static int displayed_input_sample_size = 6;
	public static int max_history = 32;
	public static int repetition_count = 2;
	public static int max_stars = 1000;
	public static int max_graph_height = 100000;
	public static int max_substitutions = 10000;
	public static int max_recursion_depth = 765; // assuming java -Xmx512M
	public static int max_recursion_count = 2048;
	public static int max_trace_length = 2048;
	public static int max_loops = 10000;
//	public static int estimated_brain_size = 5000;
//	public static int max_natural_number_digits = 10000;
	public static int brain_print_size = 100; // largest size of brain to print to System.out

	//
//	public static boolean trace_mode = true;
	public static boolean enable_external_sets = true;
//	public static boolean enable_external_maps = true;
//	public static boolean jp_tokenize = false;
	public static boolean fix_excel_csv = true;
	public static boolean enable_network_connection = true;
	public static boolean cache_sraix = false;
	public static boolean qa_test_mode = false;
	public static boolean make_verbs_sets_maps = false;

	/**
	 * get the value of a bot property.
	 *
	 * @param key property name
	 * @return property value or a string indicating the property is undefined
	 */
	String get(String key) {
		if (containsKey(key))
			return super.get(key);
		return Properties.default_property;
	}

	/**
	 * Read bot properties from a file.
	 *
	 * @param filename file containing bot properties
	 * @return count
	 */
	int getProperties(String filename) {
		int cnt = 0;
		File file = new File(filename);
		if (file.exists()) {
			log.debug("Get Properties: " + filename);
			try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
				String strLine;
				while ((strLine = br.readLine()) != null) {
					if (strLine.contains(":")) {
						final String property = strLine.substring(0, strLine.indexOf(":"));
						final String value = strLine.substring(strLine.indexOf(":") + 1);
						put(property, value);
						log.debug("load Properties key:{} value:{}", property, value);
						cnt++;
					}
				}
			} catch (final Exception e) {
				log.error("Error: " + e.getMessage());
			}
		}
		return cnt;
	}
}
