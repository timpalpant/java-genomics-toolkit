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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

public class AssemblyManagerDialog extends JDialog {
	
	public static final int DEFAULT_WIDTH = 300;
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
		model = new AssemblyTableModel(getAvailableAssemblies());
		assembliesTable.setModel(model);
		assembliesTable.setRowSelectionAllowed(false);
		assembliesTable.setColumnSelectionAllowed(false);
		assembliesTable.setCellSelectionEnabled(false);
		JScrollPane scrollPane = new JScrollPane(assembliesTable);
		assembliesTable.setFillsViewportHeight(true);
		contentPanel.add(scrollPane);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		
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
	
	/**
	 * Loads all available assemblies in the resources directory
	 * @return the assemblies available in the resources directory
	 */
	public List<Assembly> getAvailableAssemblies() {
		List<Assembly> assemblies = new ArrayList<>();
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(AssemblyConverter.ASSEMBLIES_DIR, "*.{len}")) {
      for (Path entry : stream) {
      	log.debug("Loading assembly: " + entry);
				try {
					Assembly a = new Assembly(entry);
					assemblies.add(a);
				} catch (IOException | DataFormatException e1) { 
					log.warn("Error loading assembly: " + entry);
				}
      }
		} catch (IOException e) {
			log.error("Error listing assemblies");
			e.printStackTrace();
			JOptionPane.showMessageDialog(getParent(),
			    "Error listing available assemblies",
			    "Assemblies Error",
			    JOptionPane.ERROR_MESSAGE);
		}
		
		return assemblies;
	}
	
	private void loadCustomAssembly() {
		int returnVal = fcCustomAssembly.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			Path assemblyFile = fcCustomAssembly.getSelectedFile().toPath();
			log.debug("Loading custom assembly from file: " + assemblyFile);
			Assembly a = null;
			try {
				a = new Assembly(assemblyFile);
			} catch (Exception e) {
				log.error("Error loading custom assembly: " + e.getMessage());
				e.printStackTrace();
				JOptionPane.showMessageDialog(this,
				    "Error loading custom assembly",
				    "Custom Assembly Error",
				    JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			// Check if this assembly is already loaded
			if (model.containsAssembly(a)) {
				log.warn("Overwriting assembly: " + a.getPath().getFileName());
				JOptionPane.showMessageDialog(this,
				    "Assembly will be overwritten",
				    "Warning",
				    JOptionPane.WARNING_MESSAGE);
				model.removeAssembly(a);
			}
			
			// If it loaded correctly, copy the assembly file into the built-in assemblies directory
			try {
				Files.copy(assemblyFile, AssemblyConverter.ASSEMBLIES_DIR.resolve(assemblyFile.getFileName()));
			} catch (IOException e) {
				log.error("Error copying custom assembly into assemblies directory!");
				e.printStackTrace();
				JOptionPane.showMessageDialog(this,
				    "Error copying custom assembly into resources directory",
				    "Custom Assembly Error",
				    JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			// Add it to the assemblies list model
			model.addAssembly(a);
		}
	}
	
	private void closeDialog() {
		this.dispose();
	}

}
