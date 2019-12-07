package org.alicebot.ab;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is here to simulate a Contacts database for the purpose of testing
 * contactaction.aiml
 */
class Contact {

	static int contactCount = 0;
	static HashMap<String, Contact> idContactMap = new HashMap<String, Contact>();
	static HashMap<String, String> nameIdMap = new HashMap<String, String>();
	String contactId;
	String displayName;
	String birthday;
	HashMap<String, String> phones;
	HashMap<String, String> emails;

	static String multipleIds(String contactName) {
		String patternString = " (" + contactName.toUpperCase() + ") ";
		while (patternString.contains(" ")) {
			patternString = patternString.replace(" ", "(.*)");
		}
		// log.info("Pattern='"+patternString+"'");
		final Pattern pattern = Pattern.compile(patternString);
		final Set<String> keys = nameIdMap.keySet();
		String result = "";
		int idCount = 0;
		for (final String key : keys) {
			final Matcher m = pattern.matcher(key);
			if (m.find()) {
				result += nameIdMap.get(key.toUpperCase()) + " ";
				idCount++;
			}
		}
		if (idCount <= 1) {
			result = "false";
		}
		return result.trim();
	}

	static String contactId(final String contactName) {
		String patternString = " " + contactName.toUpperCase() + " ";
		while (patternString.contains(" ")) {
			patternString = patternString.replace(" ", ".*");
		}
		// log.info("Pattern='"+patternString+"'");
		final Pattern pattern = Pattern.compile(patternString);
		final Set<String> keys = nameIdMap.keySet();
		String result = "unknown";
		for (final String key : keys) {
			final Matcher m = pattern.matcher(key);
			if (m.find()) {
				result = nameIdMap.get(key.toUpperCase()) + " ";
			}
		}
		return result.trim();
	}

	static String displayName(String id) {
		final Contact c = idContactMap.get(id.toUpperCase());
		String result = "unknown";
		if (c != null) {
			result = c.displayName;
		}
		return result;
	}

	static String dialNumber(String type, String id) {
		String result = "unknown";
		final Contact c = idContactMap.get(id.toUpperCase());
		if (c != null) {
			final String dialNumber = c.phones.get(type.toUpperCase());
			if (dialNumber != null) {
				result = dialNumber;
			}
		}
		return result;
	}

	static String emailAddress(String type, String id) {
		String result = "unknown";
		final Contact c = idContactMap.get(id.toUpperCase());
		if (c != null) {
			final String emailAddress = c.emails.get(type.toUpperCase());
			if (emailAddress != null) {
				result = emailAddress;
			}
		}
		return result;
	}

	static String birthday(String id) {
		final Contact c = idContactMap.get(id.toUpperCase());
		if (c == null) {
			return "unknown";
		} else {
			return c.birthday;
		}
	}

	Contact(String displayName, String phoneType, String dialNumber, String emailType, String emailAddress, String birthday) {
		contactId = "ID" + contactCount;
		contactCount++;
		phones = new HashMap<String, String>();
		emails = new HashMap<String, String>();
		idContactMap.put(contactId.toUpperCase(), this);
		addPhone(phoneType, dialNumber);
		addEmail(emailType, emailAddress);
		addName(displayName);
		addBirthday(birthday);
	}

	private void addPhone(String type, String dialNumber) {
		phones.put(type.toUpperCase(), dialNumber);
	}

	private void addEmail(String type, String emailAddress) {
		emails.put(type.toUpperCase(), emailAddress);
	}

	private void addName(String name) {
		displayName = name;
		nameIdMap.put(displayName.toUpperCase(), contactId);
		// log.info(nameIdMap.toString());
	}

	private void addBirthday(String birthday) {
		this.birthday = birthday;
	}

}
