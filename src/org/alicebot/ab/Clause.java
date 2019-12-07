package org.alicebot.ab;

public class Clause {

	String subj;
	String pred;
	String obj;
	Boolean affirm;

	Clause(String s, String p, String o) {
		this(s, p, o, true);
	}

	Clause(String s, String p, String o, Boolean affirm) {
		subj = s;
		pred = p;
		obj = o;
		this.affirm = affirm;
	}

	Clause(Clause clause) {
		this(clause.subj, clause.pred, clause.obj, clause.affirm);
	}

}
