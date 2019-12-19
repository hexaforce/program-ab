package org.alicebot.ab;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Tuple extends HashMap<String, String> {
	private static final long serialVersionUID = 1L;

	private static int index = 0;
	static HashMap<String, Tuple> tupleMap = new HashMap<String, Tuple>();
	private HashSet<String> visibleVars = new HashSet<String>();
	String name;

	Tuple(HashSet<String> varSet, HashSet<String> visibleVars, Tuple tuple) {
		super();
		if (visibleVars != null) {
			this.visibleVars.addAll(visibleVars);
		}
		if (varSet == null && tuple != null) {
			for (final String key : tuple.keySet()) {
				put(key, tuple.get(key));
			}
			this.visibleVars.addAll(tuple.visibleVars);
		}
		if (varSet != null) {
			for (final String key : varSet) {
				put(key, Properties.unbound_variable);
			}
		}
		name = "tuple" + index;
		index++;
		tupleMap.put(name, this);
	}

	@Override
	public boolean equals(Object o) {
		final Tuple tuple = (Tuple) o;
		if (visibleVars.size() != tuple.visibleVars.size()) {
			return false;
		}
		for (final String x : visibleVars) {
			if (!tuple.visibleVars.contains(x)) {
				return false;
			} else if (get(x) != null && !get(x).equals(tuple.get(x))) {
				return false;
			}
		}
		if (values().contains(Properties.unbound_variable)) {
			return false;
		}
		if (tuple.values().contains(Properties.unbound_variable)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (final String x : visibleVars) {
			result = 31 * result + x.hashCode();
			if (get(x) != null) {
				result = 31 * result + get(x).hashCode();
			}
		}
		return result;
	}

	Set<String> getVars() {
		return keySet();
	}

	String getValue(String var) {
		final String result = get(var);
		if (result == null) {
			return Properties.default_get;
		} else {
			return result;
		}
	}

	void bind(String var, String value) {
		if (get(var) != null && !get(var).equals(Properties.unbound_variable)) {
			log.info(var + " already bound to " + get(var));
		} else {
			put(var, value);
		}

	}

	final static String NL = System.getProperty("line.separator");

	String printTuple() {
		String result = NL;
		for (final String x : keySet()) {
			result += x + "=" + get(x) + NL;
		}
		return result.trim();
	}
}
