package edu.unc.genomics;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * @author timpalpant
 *
 */
public class AssemblyTableModel extends AbstractTableModel {
	
	private static final long serialVersionUID = 8225453782461913732L;
	
	private static final String[] COLUMN_NAMES = { "Name", "# Contigs" };
	
	private final List<Assembly> assemblies;
	
	public AssemblyTableModel(List<Assembly> assemblies) {
		this.assemblies = assemblies;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int col) {
    return COLUMN_NAMES[col];
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
	 */
	@Override
	public boolean isCellEditable(int row, int col) { 
		return false; 
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	@Override
	public int getRowCount() {
		return assemblies.size();
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return 2;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Assembly a = assemblies.get(rowIndex);
		if (columnIndex == 0) {
			return a.toString();
		} else if (columnIndex == 1) {
			return a.chromosomes().size();
		} else {
			return null;
		}
	}
	
	public boolean containsAssembly(Assembly a) {
		String aName = a.toString();
		for (Assembly assembly : assemblies) {
			if (assembly.toString().equalsIgnoreCase(aName)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void addAssembly(Assembly a) {
		assemblies.add(a);
		fireTableRowsInserted(assemblies.size()-1, assemblies.size()-1);
	}
	
	public void removeAssembly(Assembly a) {
		String aName = a.toString();
		Iterator<Assembly> it = assemblies.iterator();
		while (it.hasNext()) {
			Assembly assembly = it.next();
			if (assembly.toString().equalsIgnoreCase(aName)) {
				it.remove();
			}
		}
		fireTableDataChanged();
	}

}
