package org.alicebot.ab;

import lombok.Builder;

@Builder
class Clause {
	String subj;
	String pred;
	String obj;
	Boolean affirm;
}
