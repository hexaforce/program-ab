package org.alicebot.ab.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

/**
 * Bot Properties
 */
@Slf4j
public class BotProperties extends Properties {
	private static final long serialVersionUID = 1L;

	public BotProperties(File file) throws FileNotFoundException, IOException {
		log.debug("LOAD Properties ", file.getAbsolutePath());
		load(new FileInputStream(file));
	}

	public static String program_name_version = "Program AB 0.0.6.26 beta -- AI Foundation Reference AIML 2.1 implementation";
	public static String default_language = "JP";
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
	public static String default_bot_response = "I have no answer for that.";
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
	public static int brain_print_size = 100; // largest size of brain to print to System.out
	
	//
	public static boolean cache_sraix = false;
	public static boolean fix_excel_csv = true;
	public static boolean enable_network_connection = true;
	public static boolean enable_external_sets = true;
	
	public String getS(String key) {
		return (String) get(key);
	}

	public Integer getI(String key) {
		return (Integer) get(key);
	}

	public Boolean getB(String key) {
		return (Boolean) get(key);
	}
	
}
