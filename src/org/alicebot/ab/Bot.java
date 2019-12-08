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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.alicebot.ab.utils.DomUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.w3c.dom.Node;

import lombok.extern.slf4j.Slf4j;

/**
 * Class representing the AIML bot
 */
@Slf4j
class Bot {

	final Properties properties = new Properties();
	final PreProcessor preProcessor;
	final Graphmaster brain;
	Graphmaster learnfGraph;
	Graphmaster learnGraph;

	// Graphmaster unfinishedGraph;
	// final ArrayList<Category> categories;

	final String botName;

	HashMap<String, AIMLSet> setMap = new HashMap<String, AIMLSet>();
	HashMap<String, AIMLMap> mapMap = new HashMap<String, AIMLMap>();
	HashSet<String> pronounSet = new HashSet<String>();

	final String aimlif_path;
	final String aiml_path;
	final String config_path;
	final String log_path;
	final File sets_path;
	final File maps_path;

	/**
	 * Constructor
	 *
	 * @param name   name of bot
	 * @param path   root path of Program AB
	 * @param action Program AB action
	 * @throws IOException
	 */
	// Bot(String name, String path, String action) {
	Bot(String workingDirectory, String botName) throws IOException {
		int cnt = 0;
		this.botName = botName;
		String currentDirectory = workingDirectory + "/bots/" + botName;
		this.aiml_path = currentDirectory + "/aiml";
		this.aimlif_path = currentDirectory + "/aimlif";
		this.config_path = currentDirectory + "/config";
		this.log_path = currentDirectory + "/logs";
		this.sets_path = new File(currentDirectory + "/sets");
		this.maps_path = new File(currentDirectory + "/maps");

//		setAllPaths(path, name);
		this.brain = new Graphmaster(this, "brain");
		this.learnfGraph = new Graphmaster(this, "learnf");
		this.learnGraph = new Graphmaster(this, "learn");
		// this.unfinishedGraph = new Graphmaster(this);
		// this.categories = new ArrayList<Category>();

		preProcessor = new PreProcessor(this);
//		addProperties();
		properties.getProperties(config_path + "/properties.txt");
		cnt = addAIMLSets();
		log.debug("Loaded " + cnt + " set elements.");
		cnt = addAIMLMaps();
		log.debug("Loaded " + cnt + " map elements");
		this.pronounSet = getPronouns();
		final AIMLSet number = new AIMLSet(Properties.natural_number_set_name, this);
		setMap.put(Properties.natural_number_set_name, number);
		final AIMLMap successor = new AIMLMap(Properties.map_successor, this);
		mapMap.put(Properties.map_successor, successor);
		final AIMLMap predecessor = new AIMLMap(Properties.map_predecessor, this);
		mapMap.put(Properties.map_predecessor, predecessor);
		final AIMLMap singular = new AIMLMap(Properties.map_singular, this);
		mapMap.put(Properties.map_singular, singular);
		final AIMLMap plural = new AIMLMap(Properties.map_plural, this);
		mapMap.put(Properties.map_plural, plural);
		// log.info("setMap = "+setMap);
		final Date aimlDate = new Date(new File(aiml_path).lastModified());
		final Date aimlIFDate = new Date(new File(aimlif_path).lastModified());
		log.debug("AIML modified " + aimlDate + " AIMLIF modified " + aimlIFDate);

		if (aimlDate.after(aimlIFDate)) {
			log.debug("AIML modified after AIMLIF");
			cnt = addCategoriesFromAIML();
			writeAIMLIFFiles();
		} else {
			addCategoriesFromAIMLIF();
			if (brain.getCategories().size() == 0) {
				log.info("No AIMLIF Files found.  Looking for AIML");
				cnt = addCategoriesFromAIML();
			}
		}

		final Category c = new Category(0, "PROGRAM VERSION", "*", "*", Properties.program_name_version, "update.aiml");
		brain.addCategory(c);
		brain.nodeStats();
		learnfGraph.nodeStats();

	}

	final static String NL = System.getProperty("line.separator");

	HashSet<String> getPronouns() throws IOException {
		final HashSet<String> pronounSet = new HashSet<String>();
		final String pronouns = FileUtils.readFileToString(new File(config_path + "/pronouns.txt"), Charset.defaultCharset());
		final String[] splitPronouns = pronouns.split(NL);
		for (final String splitPronoun : splitPronouns) {
			final String p = splitPronoun.trim();
			if (p.length() > 0) {
				pronounSet.add(p);
			}
		}
		log.debug("Read pronouns: " + pronounSet);
		return pronounSet;
	}

	/**
	 * add an array list of categories with a specific file name
	 *
	 * @param file           name of AIML file
	 * @param moreCategories list of categories
	 */
	private void addMoreCategories(String file, ArrayList<Category> moreCategories) {
		if (file.contains(Properties.deleted_aiml_file)) {
			/*
			 * for (Category c : moreCategories) { //log.info("Delete "+c.getPattern());
			 * deletedGraph.addCategory(c); }
			 */

		} else if (file.contains(Properties.learnf_aiml_file)) {
			log.debug("Reading Learnf file");
			for (final Category c : moreCategories) {
				brain.addCategory(c);
				learnfGraph.addCategory(c);
				// patternGraph.addCategory(c);
			}
			// this.categories.addAll(moreCategories);
		} else {
			for (final Category c : moreCategories) {
				// log.info("Brain size="+brain.root.size());
				// brain.printgraph();
				brain.addCategory(c);
				// patternGraph.addCategory(c);
				// brain.printgraph();
			}
			// this.categories.addAll(moreCategories);
		}
	}

	private final IOFileFilter aimlFileExtension = FileFilterUtils.suffixFileFilter(".aiml");

	/**
	 * Load all brain categories from AIML directory
	 */
	private int addCategoriesFromAIML() {
		final Timer timer = new Timer();
		timer.start();
		int cnt = 0;

		final Collection<File> aimlFiles = FileUtils.listFiles(new File(aiml_path), aimlFileExtension, FileFilterUtils.trueFileFilter());
		final Map<String, Node> rootNodes = aimlFiles.parallelStream().collect(Collectors.toMap(File::getName, DomUtils::parseFile));
		for (final Entry<String, Node> x : rootNodes.entrySet()) {
			log.debug("Loading AIML files from " + x.getKey());
			final ArrayList<Category> moreCategories = AIMLProcessor.AIMLToCategories(x.getKey(), x.getValue());
			addMoreCategories(x.getKey(), moreCategories);
			cnt += moreCategories.size();
		}

		log.debug("Loaded " + cnt + " categories in " + timer.elapsedTimeSecs() + " sec");
		return cnt;
	}

	private final IOFileFilter aimlifFileExtension = FileFilterUtils.suffixFileFilter(".csv");

	/**
	 * load all brain categories from AIMLIF directory
	 * 
	 * @return count
	 */
	int addCategoriesFromAIMLIF() {
		final Timer timer = new Timer();
		timer.start();
		int cnt = 0;

		final Map<String, String> aimlifFiles = FileUtils.listFiles(new File(aimlif_path), aimlifFileExtension, FileFilterUtils.trueFileFilter()).stream().collect(Collectors.toMap(File::getName, File::getAbsolutePath));
		for (final Entry<String, String> x : aimlifFiles.entrySet()) {
			final ArrayList<Category> moreCategories = readIFCategories(x.getValue());
			cnt += moreCategories.size();
			addMoreCategories(x.getKey(), moreCategories);
		}

		log.debug("Loaded " + cnt + " categories in " + timer.elapsedTimeSecs() + " sec");
		return cnt;
	}

	/**
	 * write all AIML and AIMLIF categories
	 */
	void writeQuit() {
		writeAIMLIFFiles();
		// log.info("Wrote AIMLIF Files");
		writeAIMLFiles();
		// log.info("Wrote AIML Files");
		/*
		 * updateUnfinishedCategories(); writeUnfinishedIFCategories();
		 */
	}

	/**
	 * write categories to AIMLIF file
	 *
	 * @param cats     array list of categories
	 * @param filename AIMLIF filename
	 */
	void writeIFCategories() {

		final File existsPath = new File(aimlif_path);
		if (!existsPath.exists()) {
			existsPath.mkdir();
		}

		ArrayList<Category> cats = learnfGraph.getCategories();
		String filename = Properties.learnf_aiml_file + Properties.aimlif_file_suffix;

		File f = new File(aimlif_path + "/" + filename);
		log.debug("writeIFCategories {}", f.getPath());

		// log.info("writeIFCategories "+filename);
		BufferedWriter bw = null;
		try {
			// Construct the bw object
			bw = new BufferedWriter(new FileWriter(f));
			for (final Category category : cats) {
				bw.write(Category.categoryToIF(category));
				bw.newLine();
			}
		} catch (final FileNotFoundException ex) {
			log.error(ex.getMessage(), ex);
		} catch (final IOException ex) {
			log.error(ex.getMessage(), ex);
		} finally {
			// Close the bw
			try {
				if (bw != null) {
					bw.flush();
					bw.close();
				}
			} catch (final IOException ex) {
				log.error(ex.getMessage(), ex);
			}
		}
		existsPath.setLastModified(new Date().getTime());
	}

	/**
	 * Write all AIMLIF files from bot brain
	 */
	void writeAIMLIFFiles() {

		final File existsPath = new File(aimlif_path);
		if (!existsPath.exists()) {
			existsPath.mkdir();
		}

		final HashMap<String, BufferedWriter> fileMap = new HashMap<String, BufferedWriter>();
		final Category b = new Category(0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
		brain.addCategory(b);

		final ArrayList<Category> brainCategories = brain.getCategories();
		Collections.sort(brainCategories, Category.CATEGORY_NUMBER_COMPARATOR);
		try {
			for (final Category c : brainCategories) {
				BufferedWriter bw;
				final String fileName = c.getFilename();
				if (fileMap.containsKey(fileName)) {
					bw = fileMap.get(fileName);
				} else {
					File f = new File(aimlif_path + "/" + fileName + Properties.aimlif_file_suffix);
					log.debug("writeAIMLIFFiles {}", f.getPath());
					bw = new BufferedWriter(new FileWriter(f));
					fileMap.put(fileName, bw);
				}
				bw.write(Category.categoryToIF(c));
				bw.newLine();
			}
			for (final String key : fileMap.keySet()) {
				final BufferedWriter bw = fileMap.get(key);
				// Close the bw
				if (bw != null) {
					bw.flush();
					bw.close();
				}
			}
		} catch (final Exception ex) {
			log.error(ex.getMessage(), ex);
		}

		existsPath.setLastModified(new Date().getTime());
	}

	/**
	 * Write all AIML files. Adds categories for BUILD and DEVELOPMENT ENVIRONMENT
	 */
	void writeAIMLFiles() {

		final File existsPath = new File(aimlif_path);
		if (!existsPath.exists()) {
			existsPath.mkdir();
		}

		final HashMap<String, BufferedWriter> fileMap = new HashMap<String, BufferedWriter>();
		final Category b = new Category(0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
		brain.addCategory(b);

		final ArrayList<Category> brainCategories = brain.getCategories();
		Collections.sort(brainCategories, Category.CATEGORY_NUMBER_COMPARATOR);
		String NL = System.getProperty("line.separator");
		try {
			for (final Category c : brainCategories) {
				if (!c.getFilename().equals(Properties.null_aiml_file)) {
					// log.info("Writing "+c.getCategoryNumber()+" "+c.inputThatTopic());
					BufferedWriter bw;
					final String fileName = c.getFilename();
					if (fileMap.containsKey(fileName)) {
						bw = fileMap.get(fileName);
					} else {
						final String copyright = Utilities.getCopyright(this, fileName);
						File f = new File(aiml_path + "/" + fileName);
						log.debug("writeAIMLFiles {}", f.getPath());
						bw = new BufferedWriter(new FileWriter(f));
						fileMap.put(fileName, bw);
						bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL + "<aiml version=\"2.1\">" + NL);
						bw.write("<!--" + NL);
						bw.write(copyright + NL);
						bw.write("-->" + NL + NL);
						// bw.newLine();
					}
					bw.write(Category.categoryToAIML(c) + NL);
					// bw.newLine();
				}
			}
			for (final String key : fileMap.keySet()) {
				final BufferedWriter bw = fileMap.get(key);
				// Close the bw
				if (bw != null) {
					bw.write("</aiml>" + NL);
					bw.flush();
					bw.close();
				}
			}
		} catch (final IOException ex) {
			log.error(ex.getMessage(), ex);
		}

		new File(aiml_path).setLastModified(new Date().getTime());
	}

	/**
	 * read AIMLIF categories from a file into bot brain
	 *
	 * @param filename name of AIMLIF file
	 * @return array list of categories read
	 */
	ArrayList<Category> readIFCategories(String filename) {
		final ArrayList<Category> categories = new ArrayList<Category>();
		try {
			// Open the file that is the first
			// command line parameter
			final FileInputStream fstream = new FileInputStream(filename);
			// Get the object
			final BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				try {
					final Category c = Category.IFToCategory(strLine);
					categories.add(c);
				} catch (final Exception ex) {
					log.info("Invalid AIMLIF in " + filename + " line " + strLine);
				}
			}
			// Close the input stream
			br.close();
		} catch (final Exception e) {// Catch exception if any
			log.error("Error: " + e.getMessage());
		}
		return categories;
	}

	private final IOFileFilter setFileExtension = FileFilterUtils.suffixFileFilter(".txt");

	/**
	 * Load all AIML Sets
	 */
	int addAIMLSets() {
		int cnt = 0;
		final Timer timer = new Timer();
		timer.start();

		if (sets_path.exists()) {
			final Map<String, String> mapFiles = FileUtils.listFiles(sets_path, setFileExtension, FileFilterUtils.trueFileFilter()).stream().collect(Collectors.toMap(File::getName, File::getAbsolutePath));
			for (final Entry<String, String> x : mapFiles.entrySet()) {
				final String setName = x.getKey().substring(0, x.getKey().length() - ".txt".length());
				log.debug("Read AIML Set " + setName);
				final AIMLSet aimlSet = new AIMLSet(setName, this);
				cnt += aimlSet.readAIMLSet(this);
				setMap.put(setName, aimlSet);
			}
		}

		return cnt;
	}

	private final IOFileFilter mapFileExtension = FileFilterUtils.suffixFileFilter(".txt");

	/**
	 * Load all AIML Maps
	 */
	int addAIMLMaps() {
		int cnt = 0;
		final Timer timer = new Timer();
		timer.start();
		if (maps_path.exists()) {
			final Map<String, String> mapFiles = FileUtils.listFiles(maps_path, mapFileExtension, FileFilterUtils.trueFileFilter()).stream().collect(Collectors.toMap(File::getName, File::getAbsolutePath));
			for (final Entry<String, String> x : mapFiles.entrySet()) {
				final String mapName = x.getKey().substring(0, x.getKey().length() - ".txt".length());
				log.debug("Read AIML Map " + mapName);
				final AIMLMap aimlMap = new AIMLMap(mapName, this);
				cnt += aimlMap.readAIMLMap(this);
				mapMap.put(mapName, aimlMap);
			}
		}

		return cnt;
	}

	void deleteLearnfCategories() {
		final ArrayList<Category> learnfCategories = learnfGraph.getCategories();
		for (final Category c : learnfCategories) {
			final Nodemapper n = brain.findNode(c);
			log.info("Found node " + n + " for " + c.inputThatTopic());
			if (n != null) {
				n.category = null;
			}
		}
		learnfGraph = new Graphmaster(this, "brain");
	}

	void deleteLearnCategories() {
		final ArrayList<Category> learnCategories = learnGraph.getCategories();
		for (final Category c : learnCategories) {
			final Nodemapper n = brain.findNode(c);
			log.info("Found node " + n + " for " + c.inputThatTopic());
			if (n != null) {
				n.category = null;
			}
		}
		learnGraph = new Graphmaster(this, "brain");
	}

}
