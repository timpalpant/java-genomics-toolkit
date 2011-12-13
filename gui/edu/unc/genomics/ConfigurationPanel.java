package edu.unc.genomics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.ParameterDescription;

import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.WigFile;

/**
 * @author timpalpant
 *
 */
public class ConfigurationPanel extends JPanel {

	private static final long serialVersionUID = 3336295203155728629L;
	
	/**
	 * The model for the Job that this panel allows you to configure
	 */
	private Job job;

	/**
	 * Initialize a new ConfigurationPanel with no Job
	 */
	public ConfigurationPanel() { }
	
	/**
	 * Initialize a new ConfigurationPanel for the given Job
	 * @param job
	 */
	public ConfigurationPanel(final Job job) {
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
		// TODO: Highlight fields that are not valid / unset
	}
	
	/**
	 * Render the parameters from the Job into GUI components
	 */
	private void renderJob() {
		removeAll();
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
			fieldPanel.add(label);
			
			// Add a box for configuring the parameter based on its type
			Field field = paramDescription.getField();
			Class<?> type = field.getType();
			if (type.equals(Path.class)) {
				final JTextField textField = new JTextField();
				fieldPanel.add(textField);
				JButton btnChooseFile = new JButton("Choose File");
				btnChooseFile.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JFileChooser fc = new JFileChooser();
						int returnVal = fc.showSaveDialog(textField.getRootPane());
						if (returnVal == JFileChooser.APPROVE_OPTION) {
							textField.setText(fc.getSelectedFile().toString());
						}
					}
				});
				fieldPanel.add(btnChooseFile);
			} else if (type.equals(IntervalFile.class)) {
				JTextField textField = new JTextField();
				fieldPanel.add(textField);
			} else if (type.equals(WigFile.class)) {
				JTextField textField = new JTextField();
				fieldPanel.add(textField);
			} else if (type.equals(Assembly.class)) {
				List<Assembly> availableAssemblies = AssemblyManager.getAvailableAssemblies();
				Assembly[] assemblies = new Assembly[availableAssemblies.size()];
				assemblies = availableAssemblies.toArray(assemblies);
				final JComboBox<Assembly> cbAssemblyChooser = new JComboBox<Assembly>(assemblies);
				cbAssemblyChooser.setSelectedItem(AssemblyManager.getLastUsed());
				cbAssemblyChooser.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						AssemblyManager.setLastUsed((Assembly) cbAssemblyChooser.getSelectedItem());
					}
				});
				fieldPanel.add(cbAssemblyChooser);
			} else {
				JTextField textField = new JTextField();
				fieldPanel.add(textField);
			}
		}
		
		revalidate();
	}

}
