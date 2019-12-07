
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
	Library General Public License for more details.

	You should have received a copy of the GNU Library General Public
	License along with this library; if not, write to the
	Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
	Boston, MA  02110-1301, USA.
*/

import java.io.IOException;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;

import org.alicebot.ab.utils.BotProperties;
import org.alicebot.ab.utils.IOUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

	public static void main(String[] args) throws IOException {

//		String botName = "alice1.5";
		String botName = "alice2";
//		String botName = "アリス";
		String workingDirectory = System.getProperty("user.dir");

		log.info(BotProperties.program_name_version);
		log.debug("Working Directory = " + workingDirectory);

		final Bot bot = new Bot(workingDirectory, botName);
		if (bot.brain.getCategories().size() < BotProperties.brain_print_size) {
			bot.brain.printgraph();
		}
		final Chat chatSession = new Chat(bot, "0");

		bot.brain.nodeStats();
		String textLine = "";
		while (true) {
			textLine = IOUtils.readInputTextLine("Human");
			if (textLine == null || textLine.length() < 1) {
				textLine = BotProperties.null_input;
			}
			if (textLine.equals("q")) {
				System.exit(0);
			} else if (textLine.equals("wq")) {
				bot.writeQuit();
				System.exit(0);
			} else {
				final String request = textLine;
				log.debug("STATE=" + request + ":THAT=" + chatSession.thatHistory.get(0).get(0) + ":TOPIC=" + chatSession.predicates.get("topic"));
				String response = chatSession.multisentenceRespond(request);
				while (response.contains("&lt;")) {
					response = response.replace("&lt;", "<");
				}
				while (response.contains("&gt;")) {
					response = response.replace("&gt;", ">");
				}
				IOUtils.writeOutputTextLine("Robot", response);
			}
		}
	}

}
