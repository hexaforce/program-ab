package org.alicebot.ab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TripleStore {

	private int idCnt = 0;
	private String name = "unknown";
	private Bot bot;
	private HashMap<String, Triple> idTriple = new HashMap<String, Triple>();
	private HashMap<String, String> tripleStringId = new HashMap<String, String>();
	private HashMap<String, HashSet<String>> subjectTriples = new HashMap<String, HashSet<String>>();
	private HashMap<String, HashSet<String>> predicateTriples = new HashMap<String, HashSet<String>>();
	private HashMap<String, HashSet<String>> objectTriples = new HashMap<String, HashSet<String>>();

	TripleStore(String name, Bot bot) {
		this.name = name;
		this.bot = bot;
	}

	private class Triple {
		private String id;
		private String subject;
		private String predicate;
		private String object;

		private Triple(String s, String p, String o) {
			final Bot bot = TripleStore.this.bot;
			if (bot != null) {
				s = bot.preProcessor.normalize(s);
				p = bot.preProcessor.normalize(p);
				o = bot.preProcessor.normalize(o);
			}
			if (s != null && p != null && o != null) {
				subject = s;
				predicate = p;
				object = o;
				id = name + idCnt++;
			}
		}
	}

	private String mapTriple(Triple triple) {

		final String id = triple.id;
		idTriple.put(id, triple);
		String s, p, o;
		s = triple.subject;
		p = triple.predicate;
		o = triple.object;

		s = s.toUpperCase();
		p = p.toUpperCase();
		o = o.toUpperCase();

		String tripleString = s + ":" + p + ":" + o;
		tripleString = tripleString.toUpperCase();

		if (tripleStringId.keySet().contains(tripleString)) {
			return tripleStringId.get(tripleString); // triple already exists
		} else {

			tripleStringId.put(tripleString, id);

			HashSet<String> existingTriples;
			if (subjectTriples.containsKey(s)) {
				existingTriples = subjectTriples.get(s);
			} else {
				existingTriples = new HashSet<String>();
			}
			existingTriples.add(id);
			subjectTriples.put(s, existingTriples);

			if (predicateTriples.containsKey(p)) {
				existingTriples = predicateTriples.get(p);
			} else {
				existingTriples = new HashSet<String>();
			}
			existingTriples.add(id);
			predicateTriples.put(p, existingTriples);

			if (objectTriples.containsKey(o)) {
				existingTriples = objectTriples.get(o);
			} else {
				existingTriples = new HashSet<String>();
			}
			existingTriples.add(id);
			objectTriples.put(o, existingTriples);

			return id;
		}
	}

	private String unMapTriple(Triple triple) {

		String id = Properties.undefined_triple;
		String s, p, o;

		s = triple.subject;
		p = triple.predicate;
		o = triple.object;

		s = s.toUpperCase();
		p = p.toUpperCase();
		o = o.toUpperCase();

		String tripleString = s + ":" + p + ":" + o;

		log.info("unMapTriple " + tripleString);
		tripleString = tripleString.toUpperCase();

		triple = idTriple.get(tripleStringId.get(tripleString));

		log.info("unMapTriple " + triple);
		if (triple != null) {
			id = triple.id;
			idTriple.remove(id);
			tripleStringId.remove(tripleString);

			HashSet<String> existingTriples;
			if (subjectTriples.containsKey(s)) {
				existingTriples = subjectTriples.get(s);
			} else {
				existingTriples = new HashSet<String>();
			}
			existingTriples.remove(id);
			subjectTriples.put(s, existingTriples);

			if (predicateTriples.containsKey(p)) {
				existingTriples = predicateTriples.get(p);
			} else {
				existingTriples = new HashSet<String>();
			}
			existingTriples.remove(id);
			predicateTriples.put(p, existingTriples);

			if (objectTriples.containsKey(o)) {
				existingTriples = objectTriples.get(o);
			} else {
				existingTriples = new HashSet<String>();
			}
			existingTriples.remove(id);
			objectTriples.put(o, existingTriples);
		} else {
			id = Properties.undefined_triple;
		}
		return id;

	}

	private Set<String> allTriples() {
		return new HashSet<String>(idTriple.keySet());
	}

	String addTriple(String subject, String predicate, String object) {
		if (subject == null || predicate == null || object == null) {
			return Properties.undefined_triple;
		}
		final Triple triple = new Triple(subject, predicate, object);
		final String id = mapTriple(triple);
		return id;
	}

	String deleteTriple(String subject, String predicate, String object) {
		if (subject == null || predicate == null || object == null) {
			return Properties.undefined_triple;
		}
		log.debug("Deleting " + subject + " " + predicate + " " + object);
		final Triple triple = new Triple(subject, predicate, object);
		final String id = unMapTriple(triple);
		return id;
	}

	HashSet<String> emptySet() {
		return new HashSet<String>();
	}

	private HashSet<String> getTriples(String s, String p, String o) {

		Set<String> subjectSet;
		Set<String> predicateSet;
		Set<String> objectSet;
		Set<String> resultSet;
		log.debug("TripleStore: getTriples [" + idTriple.size() + "] " + s + ":" + p + ":" + o);

		if (s == null || s.startsWith("?")) {
			subjectSet = allTriples();
		} else {
			s = s.toUpperCase();
			if (subjectTriples.containsKey(s)) {
				subjectSet = subjectTriples.get(s);
			} else {
				subjectSet = emptySet();
			}
		}

		if (p == null || p.startsWith("?")) {
			predicateSet = allTriples();
		} else {
			p = p.toUpperCase();
			if (predicateTriples.containsKey(p)) {
				predicateSet = predicateTriples.get(p);
			} else {
				predicateSet = emptySet();
			}
		}

		if (o == null || o.startsWith("?")) {
			objectSet = allTriples();
		} else {
			o = o.toUpperCase();
			if (objectTriples.containsKey(o)) {
				objectSet = objectTriples.get(o);
			} else {
				objectSet = emptySet();
			}
		}

		resultSet = new HashSet<String>(subjectSet);
		resultSet.retainAll(predicateSet);
		resultSet.retainAll(objectSet);

		return new HashSet<String>(resultSet);
	}

	private String getSubject(String id) {
		if (idTriple.containsKey(id)) {
			return idTriple.get(id).subject;
		} else {
			return "Unknown subject";
		}
	}

	private String getPredicate(String id) {
		if (idTriple.containsKey(id)) {
			return idTriple.get(id).predicate;
		} else {
			return "Unknown predicate";
		}
	}

	private String getObject(String id) {
		if (idTriple.containsKey(id)) {
			return idTriple.get(id).object;
		} else {
			return "Unknown object";
		}
	}

	HashSet<Tuple> select(HashSet<String> vars, HashSet<String> visibleVars, ArrayList<Clause> clauses) {
		HashSet<Tuple> result = new HashSet<Tuple>();
		try {
			final Tuple tuple = new Tuple(vars, visibleVars, null);
			result = selectFromRemainingClauses(tuple, clauses);
			for (final Tuple t : result) {
				log.debug(t.printTuple());
			}
		} catch (final Exception ex) {
			log.info("Something went wrong with select " + visibleVars);
			log.error(ex.getMessage(), ex);

		}
		return result;
	}

	private Clause adjustClause(Tuple tuple, Clause clause) {
		final Set<String> vars = tuple.getVars();
		final String subj = clause.subj;
		final String pred = clause.pred;
		final String obj = clause.obj;
		final Boolean affirm = clause.affirm;
		final Clause newClause = Clause.builder().subj(subj).pred(pred).obj(obj).affirm(affirm).build();
		if (vars.contains(subj)) {
			final String value = tuple.getValue(subj);
			if (!value.equals(Properties.unbound_variable)) {
				newClause.subj = value;
			}
		}
		if (vars.contains(pred)) {
			final String value = tuple.getValue(pred);
			if (!value.equals(Properties.unbound_variable)) {
				newClause.pred = value;
			}
		}
		if (vars.contains(obj)) {
			final String value = tuple.getValue(obj);
			if (!value.equals(Properties.unbound_variable)) {
				newClause.obj = value;
			}
		}
		return newClause;

	}

	private Tuple bindTuple(Tuple partial, String triple, Clause clause) {
		final Tuple tuple = new Tuple(null, null, partial);
		if (clause.subj.startsWith("?")) {
			tuple.bind(clause.subj, getSubject(triple));
		}
		if (clause.pred.startsWith("?")) {
			tuple.bind(clause.pred, getPredicate(triple));
		}
		if (clause.obj.startsWith("?")) {
			tuple.bind(clause.obj, getObject(triple));
		}
		return tuple;
	}

	HashSet<Tuple> selectFromSingleClause(Tuple partial, Clause clause, Boolean affirm) {

		final HashSet<Tuple> result = new HashSet<Tuple>();
		final HashSet<String> triples = getTriples(clause.subj, clause.pred, clause.obj);

		if (affirm) {
			for (final String triple : triples) {
				final Tuple tuple = bindTuple(partial, triple, clause);
				result.add(tuple);
			}
		} else {
			if (triples.size() == 0) {
				result.add(partial);
			}
		}
		return result;
	}

	private HashSet<Tuple> selectFromRemainingClauses(Tuple partial, ArrayList<Clause> clauses) {

		HashSet<Tuple> result = new HashSet<Tuple>();
		Clause clause = clauses.get(0);
		clause = adjustClause(partial, clause);
		final HashSet<Tuple> tuples = selectFromSingleClause(partial, clause, clause.affirm);
		if (clauses.size() > 1) {
			final ArrayList<Clause> remainingClauses = new ArrayList<Clause>(clauses);
			remainingClauses.remove(0);
			for (final Tuple tuple : tuples) {
				result.addAll(selectFromRemainingClauses(tuple, remainingClauses));
			}
		} else {
			result = tuples;
		}
		return result;
	}

}
