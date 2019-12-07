package org.alicebot.ab.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IOUtils {

	BufferedReader reader;
	BufferedWriter writer;

	public IOUtils(String filePath, String mode) {
		try {
			if (mode.equals("read")) {
				reader = new BufferedReader(new FileReader(filePath));
			} else if (mode.equals("write")) {
				(new File(filePath)).delete();
				writer = new BufferedWriter(new FileWriter(filePath, true));
			}
		} catch (IOException e) {
			log.error("error: " + e);
		}
	}

	public String readLine() {
		String result = null;
		try {
			result = reader.readLine();
		} catch (IOException e) {
			log.error("error: " + e);
		}
		return result;
	}

	public void writeLine(String line) {
		try {
			writer.write(line);
			writer.newLine();
		} catch (IOException e) {
			log.error("error: " + e);
		}
	}

	public void close() {
		try {
			if (reader != null)
				reader.close();
			if (writer != null)
				writer.close();
		} catch (IOException e) {
			log.error("error: " + e);
		}

	}

	public static void writeOutputTextLine(String prompt, String text) {
		log.info(prompt + ": " + text);
	}

	public static String readInputTextLine() {
		return readInputTextLine(null);
	}

	public static String readInputTextLine(String prompt) {
		if (prompt != null) {
			System.out.print(prompt + ": ");
		}
		BufferedReader lineOfText = new BufferedReader(new InputStreamReader(System.in));
		String textLine = null;
		try {
			textLine = lineOfText.readLine();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return textLine;
	}

	public static File[] listFiles(File dir) {
		return dir.listFiles();
	}

	final static String NL = System.getProperty("line.separator");
	
	public static String system(String evaluatedContents, String failedString) {
		Runtime rt = Runtime.getRuntime();
		try {
			Process p = rt.exec(evaluatedContents);
			InputStream istrm = p.getInputStream();
			InputStreamReader istrmrdr = new InputStreamReader(istrm);
			BufferedReader buffrdr = new BufferedReader(istrmrdr);
			String result = "";
			String data = "";
			while ((data = buffrdr.readLine()) != null) {
				result += data + NL;
			}
			return result;
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return failedString;
		}
	}

	public static String evalScript(String engineName, String script) throws Exception {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");
		String result = "" + engine.eval(script);
		return result;
	}

}
