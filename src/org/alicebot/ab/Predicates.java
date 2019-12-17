package org.alicebot.ab;

import java.util.HashMap;

import org.alicebot.ab.utils.JapaneseUtils;

/**
 * Manage client predicates
 *
 */
class Predicates extends HashMap<String, String> {
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
		if (key.equals("topic")) {
			value = JapaneseUtils.tokenizeSentence(value);
		}
		if (key.equals("topic") && value.length() == 0) {
			value = Properties.default_get;
		}
		if (value.equals(Properties.too_much_recursion)) {
			value = Properties.default_list_item;
		}
		return super.put(key, value);
	}

	/**
	 * get a predicate value
	 *
	 * @param key predicate name
	 * @return predicate value
	 */
	String get(String key) {
		if (this.containsKey(key)) {
			return super.get(key);
		}
		return Properties.default_get;
	}

}
