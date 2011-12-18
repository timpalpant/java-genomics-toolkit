package edu.unc.genomics;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;

/**
 * Tree view of the available tools
 * 
 * @author timpalpant
 *
 */
public class ToolsTree extends JTree {

	private static final long serialVersionUID = -2591915754191263660L;

	public ToolsTree() {
		super();
		initialize();
	}
	
	public ToolsTree(ToolsTreeModel model) {
		super(model);
		initialize();
	}
	
	private void initialize() {
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		
		setBorder(BorderFactory.createEmptyBorder());
		setRootVisible(false);
		setShowsRootHandles(true);
		setPreferredSize(new Dimension(200, 0));
	}
	
}
