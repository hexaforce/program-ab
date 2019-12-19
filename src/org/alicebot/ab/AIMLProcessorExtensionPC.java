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

import java.util.Set;

import org.w3c.dom.Node;

import lombok.extern.slf4j.Slf4j;

/**
 * This is just a stub to make the contactaction.aiml file work on a PC with
 * some extension tags that are defined for mobile devices.
 */
@Slf4j
class AIMLProcessorExtensionPC implements AIMLProcessorExtension {

	Set<String> extensionTagNames = Utilities.stringSet("contactid", "multipleids", "displayname", "dialnumber", "emailaddress", "contactbirthday", "addinfo");

	@Override
	public Set<String> extensionTagSet() {
		return extensionTagNames;
	}

	private String newContact(Node node, ParseState ps) {
		
		String emailAddress = "unknown";
		String displayName = "unknown";
		String dialNumber = "unknown";
		String emailType = "unknown";
		String phoneType = "unknown";
		String birthday = "unknown";

		for (Node child : new IterableNodeList(node.getChildNodes())) {
			String content = AIMLProcessor.evalTagContent(child, ps, null);;
			if (child.getNodeName().equals("birthday")) {
				birthday = content;
			} else if (child.getNodeName().equals("phonetype")) {
				phoneType = content;
			} else if (child.getNodeName().equals("emailtype")) {
				emailType = content;
			} else if (child.getNodeName().equals("dialnumber")) {
				dialNumber = content;
			} else if (child.getNodeName().equals("displayname")) {
				displayName = content;
			} else if (child.getNodeName().equals("emailaddress")) {
				emailAddress = content;
			}
		}
		
		log.info("Adding new contact " + displayName + " " + phoneType + " " + dialNumber + " " + emailType + " " + emailAddress + " " + birthday);
		new Contact(displayName, phoneType, dialNumber, emailType, emailAddress, birthday);
		return "";
	}

	private String contactId(Node node, ParseState ps) {
		final String displayName = AIMLProcessor.evalTagContent(node, ps, null);
		final String result = Contact.contactId(displayName);
		return result;
	}

	private String multipleIds(Node node, ParseState ps) {
		final String contactName = AIMLProcessor.evalTagContent(node, ps, null);
		final String result = Contact.multipleIds(contactName);
		return result;
	}

	private String displayName(Node node, ParseState ps) {
		final String id = AIMLProcessor.evalTagContent(node, ps, null);
		final String result = Contact.displayName(id);
		return result;
	}

	private String dialNumber(Node node, ParseState ps) {
		String id = "unknown";
		String type = "unknown";
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			String content = AIMLProcessor.evalTagContent(child, ps, null);
			if (child.getNodeName().equals("id")) {
				id = content;
			} else if (child.getNodeName().equals("type")) {
				type = content;
			}
		}
		return Contact.dialNumber(type, id);
	}

	private String emailAddress(Node node, ParseState ps) {
		String id = "unknown";
		String type = "unknown";
		for (Node child : new IterableNodeList(node.getChildNodes())) {
			String content = AIMLProcessor.evalTagContent(child, ps, null);
			if (child.getNodeName().equals("id")) {
				id = content;
			} else if (child.getNodeName().equals("type")) {
				type = content;
			}
		}
		return Contact.emailAddress(type, id);
	}

	private String contactBirthday(Node node, ParseState ps) {
		final String id = AIMLProcessor.evalTagContent(node, ps, null);
		final String result = Contact.birthday(id);
		return result;
	}

	@Override
	public String recursEval(Node node, ParseState ps) {
		try {
			final String nodeName = node.getNodeName();
			if (nodeName.equals("contactid")) {
				return contactId(node, ps);
			} else if (nodeName.equals("multipleids")) {
				return multipleIds(node, ps);
			} else if (nodeName.equals("dialnumber")) {
				return dialNumber(node, ps);
			} else if (nodeName.equals("addinfo")) {
				return newContact(node, ps);
			} else if (nodeName.equals("displayname")) {
				return displayName(node, ps);
			} else if (nodeName.equals("emailaddress")) {
				return emailAddress(node, ps);
			} else if (nodeName.equals("contactbirthday")) {
				return contactBirthday(node, ps);
			} else {
				return (AIMLProcessor.genericXML(node, ps));
			}
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
			return "";
		}
	}

}
