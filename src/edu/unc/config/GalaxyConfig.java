package edu.unc.config;

import java.io.IOException;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class GalaxyConfig {
	
	/**
	 * Parse a Galaxy configuration file
	 * @param p
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static GalaxyConfig parse(Path p) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(p.toFile());
		return parse(doc);
	}
	
	/**
	 * Parse a Galaxy configuration XML
	 * @param doc
	 * @return
	 */
	public static GalaxyConfig parse(Document doc) {
		GalaxyConfig config = new GalaxyConfig();
		
		// TODO Implement parser
		
		return config;
	}
}
