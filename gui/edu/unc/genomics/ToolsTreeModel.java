package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Model for the ToolsTree
 * Essentially just a DefaultTreeModel, but provides methods for
 * loading the available tools from a configuration file
 * 
 * @author timpalpant
 *
 */
public class ToolsTreeModel extends DefaultTreeModel {
	
	public static final Path DEFAULT_CONFIGURATION_FILE = Paths.get("toolConf.xml");
	
	private static final Logger log = Logger.getLogger(ToolsTreeModel.class);

	private static final long serialVersionUID = -6587614270922489960L;

	public ToolsTreeModel() {
		super(new DefaultMutableTreeNode("Tools"));
	}
	
	public static ToolsTreeModel loadDefaultConfig() throws ClassNotFoundException, ParserConfigurationException, SAXException, IOException {
		return loadConfig(DEFAULT_CONFIGURATION_FILE);
	}
	
	public static ToolsTreeModel loadConfig(Path p) throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException {
		ToolsTreeModel model = new ToolsTreeModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		
		// Populate the TreeModel with the tools in the default configuration file
		log.debug("Loading tools from: " + DEFAULT_CONFIGURATION_FILE.toAbsolutePath());
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(p.toFile());
		
		// Iterate over the sections
		NodeList sections = doc.getElementsByTagName("section");
		log.debug("Found "+sections.getLength()+" sections");
		for (int i = 0; i < sections.getLength(); i++) {
			Node section = sections.item(i);
			String sectionName = section.getAttributes().getNamedItem("name").getNodeValue();
			log.debug("Loading section: " + sectionName);
			DefaultMutableTreeNode sectionNode = new DefaultMutableTreeNode(sectionName);
			root.add(sectionNode);
			NodeList tools = section.getChildNodes();
			
			// Iterate over the tools in each section
			for (int j = 0; j < tools.getLength(); j++) {
				Node tool = tools.item(j);
				if (tool.getNodeType() == Node.ELEMENT_NODE && tool.getNodeName().equalsIgnoreCase("tool")) {
					String toolName = tool.getAttributes().getNamedItem("name").getNodeValue();
					log.debug("Loading tool: " + toolName);
					String toolClassName = tool.getAttributes().getNamedItem("class").getNodeValue();
					Class<? extends CommandLineTool> toolClass = (Class<? extends CommandLineTool>) Class.forName(toolClassName);
					ToolsTreeNode toolNode = new ToolsTreeNode(toolName, toolClass);
					sectionNode.add(toolNode);
				}
			}
		}
		
		return model;
	}

}
