package edu.unc.genomics;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;

import org.apache.log4j.Logger;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.DataFormatException;

/**
 * View for AssemblyManager
 * 
 * @author timpalpant
 *
 */
public class AssemblyManagerDialog extends JDialog {
	
	public static final int DEFAULT_WIDTH = 400;
	public static final int DEFAULT_HEIGHT = 500;
	
	private static final long serialVersionUID = -1461628562713621064L;
	private static final Logger log = Logger.getLogger(AssemblyManagerDialog.class);

	private final JPanel contentPanel = new JPanel();
	private final JFileChooser fcCustomAssembly = new JFileChooser();
	private final JTable assembliesTable = new JTable();
	
	private AssemblyTableModel model;

	/**
	 * Create the dialog.
	 */
	public AssemblyManagerDialog(JFrame parent) {
		super(parent, "Assembly Manager", true);
		
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		int centeredX = parent.getX() + (parent.getWidth()-getWidth()) / 2;
		int centeredY = parent.getY() + (parent.getHeight()-getHeight()) / 2;
		setLocation(centeredX, centeredY);
		
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));

		// Initialize the assemblies list
		model = new AssemblyTableModel(AssemblyManager.getAvailableAssemblies());
		assembliesTable.setModel(model);
		assembliesTable.setRowSelectionAllowed(true);
		JScrollPane scrollPane = new JScrollPane(assembliesTable);
		assembliesTable.setFillsViewportHeight(true);
		contentPanel.add(scrollPane);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		
		JButton removeAssemblyButton = new JButton("Remove");
		removeAssemblyButton.setActionCommand("RemoveAssembly");
		removeAssemblyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeCustomAssembly();
			}
		});
		buttonPane.add(removeAssemblyButton);
		
		JButton loadAssemblyButton = new JButton("Load Custom Assembly");
		loadAssemblyButton.setActionCommand("LoadAssembly");
		loadAssemblyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadCustomAssembly();
			}
		});
		buttonPane.add(loadAssemblyButton);
		
		JButton doneButton = new JButton("Done");
		doneButton.setActionCommand("Done");
		doneButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeDialog();
			}
		});
		buttonPane.add(doneButton);
		getRootPane().setDefaultButton(doneButton);
	}
	
	private void removeCustomAssembly() {
		for (int row : assembliesTable.getSelectedRows()) {
			try {
				Assembly a = model.getRow(row);
				AssemblyManager.deleteAssembly(a);
				model.removeRow(row);
			} catch (IOException e) {
				log.error("Error deleting Assembly");
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error deleting assembly", "Assembly Manager Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void loadCustomAssembly() {
		int returnVal = fcCustomAssembly.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			Path assemblyFile = fcCustomAssembly.getSelectedFile().toPath();
			try {
				Assembly a = AssemblyManager.loadCustomAssembly(assemblyFile);
				// Add it to the assemblies list model
				model.addAssembly(a);
			} catch (IOException | DataFormatException e) {
				log.error("Error loading custom assembly: " + assemblyFile);
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error loading custom assembly", "Assembly Manager Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void closeDialog() {
		this.dispose();
	}

}
