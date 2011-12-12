/**
 * 
 */
package edu.unc.genomics;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * @author timpalpant
 *
 */
public class ToolsTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = -1070734806062499430L;
	
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		
		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		
		// Since we cannot override Class.toString()
		// Have the renderer display Classes as their simple name
		if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
      Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
      if (userObject instanceof Class) {
      	String labelText = ((Class<?>) userObject).getSimpleName();
      	setText(labelText);
      }
		}
		
		return this;
	}

}
