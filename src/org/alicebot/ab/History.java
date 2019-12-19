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

/**
 * History object to maintain history of input, that request and response
 *
 * @param <T> type of history object
 */
class History<T> {

	private Object[] history;
	@SuppressWarnings("unused")
	private String name;

	/**
	 * Constructor with default history name
	 */
	History() {
		this("unknown");
	}

	/**
	 * Constructor with history name
	 *
	 * @param name name of history
	 */
	History(String name) {
		this.name = name;
		history = new Object[Properties.max_history];
	}

	/**
	 * add an item to history
	 *
	 * @param item history item to add
	 */
	void add(T item) {
		for (int i = Properties.max_history - 1; i > 0; i--) {
			history[i] = history[i - 1];
		}
		history[0] = item;
	}

	/**
	 * get an item from history
	 *
	 * @param index history index
	 * @return history item
	 */
	@SuppressWarnings("unchecked")
	T get(int index) {
		if (index < Properties.max_history) {
			if (history[index] == null) {
				return null;
			} else {
				return (T) history[index];
			}
		} else {
			return null;
		}
	}

	/**
	 * get a String history item
	 *
	 * @param index history index
	 * @return history item
	 */
	String getString(int index) {
		if (index < Properties.max_history) {
			if (history[index] == null) {
				return Properties.unknown_history_item;
			} else {
				return (String) history[index];
			}
		} else {
			return null;
		}
	}

}
