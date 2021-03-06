package org.alicebot.ab;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.alicebot.ab.utils.BotProperties;

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
		// log.info("varSet="+varSet);
		// log.info("visbileVars="+visibleVars);
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
				put(key, BotProperties.unbound_variable);
			}
		}
		name = "tuple" + index;
		index++;
		tupleMap.put(name, this);
	}

	@Override
	public boolean equals(Object o) {
		// log.info("Calling equals");
		// if (this == o) return true;
		/*
		 * if (o == null || getClass() != o.getClass()) { log.info("unequal 1"); return
		 * false; } if (!super.equals(o)) { log.info("unequal 2"); return false; }
		 */
		final Tuple tuple = (Tuple) o;

		// if (!visibleVars.equals(tuple.visibleVars)) return false;
		if (visibleVars.size() != tuple.visibleVars.size()) {
			// log.info("Tuple: "+name+"!="+tuple.name+" because size
			// "+visibleVars.size()+"!="+tuple.visibleVars.size());
			return false;
		}
		// log.info("Tuple visibleVars = "+visibleVars+" tuple.visibleVars =
		// "+tuple.visibleVars);
		for (final String x : visibleVars) {
			// log.info("Tuple:
			// get("+x+")="+get(x)+"tuple.get("+x+")="+tuple.get(x));
			if (!tuple.visibleVars.contains(x)) {
				// log.info("Tuple: "+name+"!="+tuple.name+" because
				// !tuple.visibleVars.contains("+x+")");
				return false;
			} else if (get(x) != null && !get(x).equals(tuple.get(x))) {
				// log.info("Tuple: "+name+"!="+tuple.name+" because
				// get("+x+")="+get(x)+" and tuple.get("+x+")="+tuple.get(x));
				return false;
			}
		}
		// log.info("Tuple: values = "+values());
		// log.info("Tuple: tuple.values = "+tuple.values());
		if (values().contains(BotProperties.unbound_variable)) {
			return false;
		}
		if (tuple.values().contains(BotProperties.unbound_variable)) {
			return false;
		}
		// log.info("Tuple: "+name+"="+tuple.name);
		return true;
	}

	@Override
	public int hashCode() {
		// log.info("Calling hashCode");
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
			return BotProperties.default_get;
		} else {
			return result;
		}
	}

	void bind(String var, String value) {
		if (get(var) != null && !get(var).equals(BotProperties.unbound_variable)) {
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
