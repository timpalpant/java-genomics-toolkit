package edu.unc.genomics;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.beust.jcommander.ParameterDescription;

import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.WigFile;

/**
 * View for configuring the parameters of a Job
 * Implements databinding between a Job object and the various Swing components
 * for configuring each parameter
 * 
 * @author timpalpant
 *
 */
public class JobConfigPanel extends JPanel {

	private static final long serialVersionUID = 3336295203155728629L;
	private static final Logger log = Logger.getLogger(JobConfigPanel.class);
	
	/**
	 * Maps parameters in the Job to GUI components (forward data-binding)
	 */
	private Map<ParameterDescription, JComponent> guiMap = new HashMap<>();
	
	/**
	 * Maps GUI components to parameters in the Job (reverse data-binding)
	 */
	private Map<Component, ParameterDescription> jobMap = new HashMap<>();
	
	/**
	 * The model for the Job that this panel allows you to configure
	 */
	private Job job;

	/**
	 * Initialize a new ConfigurationPanel with no Job
	 */
	public JobConfigPanel() { }
	
	/**
	 * Initialize a new ConfigurationPanel for the given Job
	 * @param job
	 */
	public JobConfigPanel(final Job job) {
		setJob(job);
	}
	
	/**
	 * Return the Job that this ConfigurationPanel is editing
	 * @return
	 */
	public Job getJob() {
		return job;
	}
	
	/**
	 * Set the job for this Configuration panel and re-render
	 * @param job
	 */
	public void setJob(final Job job) {
		this.job = job;
		renderJob();
	}
	
	/**
	 * Highlights fields on the Panel that are not set correctly
	 */
	public void highlightInvalidArguments() {
		for (ParameterDescription param : job) {
			JComponent guiComponent = guiMap.get(param);
			if (param.getParameter().required() && !job.isSet(param)) {
				guiComponent.setBorder(BorderFactory.createLineBorder(Color.RED));
			} else {
				guiComponent.setBorder(BorderFactory.createEmptyBorder());
			}
		}
	}
	
	/**
	 * Render the parameters from the Job into GUI components
	 * and set up one-way data binding to map changes to the GUI fields
	 * back into the Job object's parameters
	 */
	private void renderJob() {
		removeAll();
		guiMap.clear();
		jobMap.clear();
		if (job == null) return;
		
		// Iterate through the parameters in the Job
		// and render them appropriately based on their type
		for (ParameterDescription paramDescription : job) {
			JPanel fieldPanel = new JPanel();
			fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.LINE_AXIS));
			add(fieldPanel);
			
			// Add the parameter name to the configuration panel
			String name = paramDescription.getLongestName();
			while (name.startsWith("-")) {
				name = name.substring(1);
			}
			name = StringUtils.capitalize(name);
			JLabel label = new JLabel(name);
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
			fieldPanel.add(label);
			
			// Add a box for configuring the parameter
			Field field = paramDescription.getField();
			Class<?> type = field.getType();
			if (type.equals(Assembly.class)) {
				List<Assembly> availableAssemblies = AssemblyManager.getAvailableAssemblies();
				Assembly[] assemblies = new Assembly[availableAssemblies.size()];
				assemblies = availableAssemblies.toArray(assemblies);
				final JComboBox<Assembly> cbAssemblyChooser = new JComboBox<Assembly>(assemblies);
				cbAssemblyChooser.setPreferredSize(new Dimension(0, 25));
				cbAssemblyChooser.setMaximumSize(new Dimension(Integer.MAX_VALUE, cbAssemblyChooser.getPreferredSize().height));
				cbAssemblyChooser.setSelectedItem(AssemblyManager.getLastUsed());
				cbAssemblyChooser.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						log.debug("Auto-databinding changed assembly into Job argument");
						Assembly selectedAssembly = (Assembly) cbAssemblyChooser.getSelectedItem();
						AssemblyManager.setLastUsed(selectedAssembly);
						ParameterDescription param = jobMap.get(cbAssemblyChooser);
						job.setArgument(param, selectedAssembly.toString());
					}
				});
				fieldPanel.add(cbAssemblyChooser);
				guiMap.put(paramDescription, cbAssemblyChooser);
				jobMap.put(cbAssemblyChooser, paramDescription);
			} else {
				final JTextField textField = new JTextField();
				textField.setPreferredSize(new Dimension(0, 25));
				textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, textField.getPreferredSize().height));
				textField.getDocument().addDocumentListener(new DocumentListener() {
					public void changedUpdate(DocumentEvent e) {
						pushTextToModel(e);
					}
					
					public void removeUpdate(DocumentEvent e) {
						pushTextToModel(e);
					}
					
					public void insertUpdate(DocumentEvent e) {
						pushTextToModel(e);
					}
					
					private void pushTextToModel(DocumentEvent e) {
						log.debug("Auto-databinding changed text into Job argument");
						Document doc = (Document) e.getDocument();
						ParameterDescription param = jobMap.get(textField);
						try {
							String text = doc.getText(0, doc.getLength());
							job.setArgument(param, text);
						} catch (BadLocationException e1) {
							log.error("Error pushing changed text into Job model");
							e1.printStackTrace();
						}
					}
				});
				fieldPanel.add(textField);
				guiMap.put(paramDescription, textField);
				jobMap.put(textField, paramDescription);
				
				// For input/output files, add a file chooser button
				if (type.equals(Path.class) || type.equals(WigFile.class) || type.equals(IntervalFile.class)) {
					// TODO Replace with file icon
					JButton btnChooseFile = new JButton("Choose File");
					btnChooseFile.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							JFileChooser fc = new JFileChooser();
							int returnVal = fc.showDialog(textField.getRootPane(), "OK");
							if (returnVal == JFileChooser.APPROVE_OPTION) {
								textField.setText(fc.getSelectedFile().toString());
							}
						}
					});
					fieldPanel.add(btnChooseFile);
				}
			}
		}
		
		revalidate();
	}

}
