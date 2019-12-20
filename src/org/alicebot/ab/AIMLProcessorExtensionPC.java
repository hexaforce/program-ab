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

	enum ExtensionTag1 {
		birthday, phonetype, emailtype, dialnumber, displayname, emailaddress;
	}

	private String newContact(Node node, ParseState ps) {

		String emailAddress = "unknown";
		String displayName = "unknown";
		String dialNumber = "unknown";
		String emailType = "unknown";
		String phoneType = "unknown";
		String birthday = "unknown";

		for (Node child : new IterableNodeList(node.getChildNodes())) {
			String content = AIMLProcessor.evalTagContent(child, ps, null);
			try {
				switch(ExtensionTag1.valueOf(child.getNodeName())) {
				case birthday:
					birthday = content;
					break;
				case dialnumber:
					dialNumber = content;
					break;
				case displayname:
					displayName = content;
					break;
				case emailaddress:
					emailAddress = content;
					break;
				case emailtype:
					emailType = content;
					break;
				case phonetype:
					phoneType = content;
					break;
				default:
					break;
				}
			} catch (IllegalArgumentException e) {
				
			}
		}

		log.info("Adding new contact " + displayName + " " + phoneType + " " + dialNumber + " " + emailType + " " + emailAddress + " " + birthday);
		new Contact(displayName, phoneType, dialNumber, emailType, emailAddress, birthday);
		return "";
	}

	enum ExtensionTag2 {
		contactid, multipleids, dialnumber, addinfo, displayname, emailaddress, contactbirthday;
	}

	@Override
	public String recursEval(Node node, ParseState ps) {

		try {
			ExtensionTag2 tag = ExtensionTag2.valueOf(node.getNodeName());
			String content = AIMLProcessor.evalTagContent(node, ps, null);
			switch (tag) {
			case addinfo:
				return newContact(node, ps);
			case contactbirthday:
				return Contact.birthday(content);
			case contactid:
				return Contact.contactId(content);
			case dialnumber:
				return Contact.dialNumber(node, ps);
			case displayname:
				return Contact.displayName(content);
			case emailaddress:
				return Contact.emailAddress(node, ps);
			case multipleids:
				return Contact.multipleIds(content);
			default:
				return AIMLProcessor.genericXML(node, ps);
			}
		} catch (IllegalArgumentException e) {
			try {
				return AIMLProcessor.genericXML(node, ps);
			} catch (final Exception ex) {
				log.error(ex.getMessage(), ex);
				return "";
			}
		}

	}

}
