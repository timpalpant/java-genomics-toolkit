package edu.unc.genomics;

import java.awt.Dimension;
import java.io.IOException;

import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

/**
 * @author timpalpant
 *
 */
public class ToolsTree extends JTree {

	private static final Logger log = Logger.getLogger(ToolsTree.class);
	private static final long serialVersionUID = -2591915754191263660L;
	
	private ToolsTreeModel model;

	public ToolsTree() {
		try {
			model = new ToolsTreeModel();
			setModel(model);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("Error loading tool configuration file!");
			e.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			log.error("Error loading tool: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		
    setCellRenderer(new ToolsTreeCellRenderer());
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		
		setRootVisible(false);
		setShowsRootHandles(true);
		setPreferredSize(new Dimension(200, 0));
	}
	
}
