package edu.unc.genomics;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author timpalpant
 *
 */
public class ToolsTreeNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = -9067416927466519457L;

	private final String name;
	private final Class<? extends CommandLineTool> clazz;

	/**
	 * @param userObject
	 */
	public ToolsTreeNode(String name, Class<? extends CommandLineTool> clazz) {
		super(clazz);
		
		this.name = name;
		this.clazz = clazz;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the clazz
	 */
	public Class<? extends CommandLineTool> getClazz() {
		return clazz;
	}

}
