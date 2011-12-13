package edu.unc.genomics;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;

import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;

import edu.unc.genomics.converters.IntervalToWig;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.WigFile;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.simplericity.macify.eawt.Application;
import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;
import org.simplericity.macify.eawt.DefaultApplication;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

public class ToolRunnerFrame extends JFrame implements ApplicationListener {
	
	private static final long serialVersionUID = 6454774196137357898L;
	private static final Logger log = Logger.getLogger(ToolRunnerFrame.class);
	
	private final JPanel contentPane = new JPanel();
	private final JProgressBar progressBar = new JProgressBar();
	private final JPanel configurationPanel = new JPanel();
	private final JTextPane helpTextPanel = new JTextPane();
	private final JTree toolsTree;
	private final JList<Job> queueList = new JList<>();
	
	private final AssemblyManagerDialog manager = new AssemblyManagerDialog(this);
	private Assembly lastUsedAssembly = null;
	
	private Class<? extends CommandLineTool> currentTool = null;

	/**
	 * Create the frame.
	 */
	public ToolRunnerFrame() {
		Application application  = new DefaultApplication();
    //application.addPreferencesMenuItem();
    //application.setEnabledPreferencesMenu(true);
    application.addApplicationListener(this);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Genomics Toolkit Tool Runner");
		setBounds(100, 100, 1000, 600);
		
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JPanel queuePanel = new JPanel();
		queuePanel.setBorder(new EmptyBorder(5, 0, 0, 0));
		queuePanel.setLayout(new BoxLayout(queuePanel, BoxLayout.PAGE_AXIS));
		contentPane.add(queuePanel, BorderLayout.EAST);
		
		JLabel queueLabel = new JLabel("Job Queue");
		queueLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		queuePanel.add(queueLabel);
		
		queueList.setPreferredSize(new Dimension(200, 0));
		queuePanel.add(queueList);
		
		JSplitPane splitPane = new JSplitPane();
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Tools");
		initializeToolsTree(rootNode);
    toolsTree = new JTree(rootNode);
    toolsTree.setCellRenderer(new ToolsTreeCellRenderer());
		toolsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		toolsTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				changeTool();
			}
		});
		toolsTree.setRootVisible(false);
		toolsTree.setShowsRootHandles(true);
		toolsTree.setPreferredSize(new Dimension(200, 0));
		JScrollPane toolsTreeScrollPane = new JScrollPane(toolsTree);
		splitPane.setLeftComponent(toolsTreeScrollPane);
		
		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
		splitPane.setRightComponent(tabbedPane);
		
		JScrollPane configScrollPane = new JScrollPane(configurationPanel);
		tabbedPane.addTab("Tool Configuration", null, configScrollPane, "Configure tool");
		configurationPanel.setLayout(new BoxLayout(configurationPanel, BoxLayout.PAGE_AXIS));
		configurationPanel.setBackground(tabbedPane.getBackground());
		

		JPanel helpPanel = new JPanel();
		tabbedPane.addTab("Help", null, helpPanel, null);
		helpPanel.setLayout(new BorderLayout(0, 0));
		
		helpTextPanel.setEditable(false);
		helpTextPanel.setBackground(tabbedPane.getBackground());
		JScrollPane helpScrollPane = new JScrollPane(helpTextPanel);
		helpPanel.add(helpScrollPane);
		
		JPanel runPanel = new JPanel();
		runPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
		contentPane.add(runPanel, BorderLayout.SOUTH);
		runPanel.setLayout(new BoxLayout(runPanel, BoxLayout.X_AXIS));
		
		runPanel.add(progressBar);
		
		JButton btnRun = new JButton("Run");
		btnRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runTool();
			}
		});
		runPanel.add(btnRun);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFileMenu = new JMenu("File");
		menuBar.add(mnFileMenu);
		
		JMenuItem mntmAssemblyManager = new JMenuItem("Assembly manager");
		mntmAssemblyManager.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manager.setVisible(true);
			}
		});
		mnFileMenu.add(mntmAssemblyManager);
		
		JMenu mnHelpMenu = new JMenu("Help");
		menuBar.add(mnHelpMenu);
		
		JMenuItem mntmHelpContents = new JMenuItem("Help Contents");
		mnHelpMenu.add(mntmHelpContents);
		
		if (!application.isMac()) {
			JMenuItem mntmAbout = new JMenuItem("About");
			mnHelpMenu.add(mntmAbout);
			mnHelpMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					handleAbout(null);
				}
			});
		}
	}
	
	private void initializeToolsTree(DefaultMutableTreeNode rootNode) {
		DefaultMutableTreeNode converters = new DefaultMutableTreeNode("Converters");
		rootNode.add(converters);
		DefaultMutableTreeNode tool = new DefaultMutableTreeNode(IntervalToWig.class);
		converters.add(tool);
	}

	private void changeTool() {
		// Returns the last path element of the selection.
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) toolsTree.getLastSelectedPathComponent();
    // Nothing is selected
    if (node == null) {   
    	return;
    }

    if (node.isLeaf()) {
      Class<? extends CommandLineTool> tool = (Class<? extends CommandLineTool>) node.getUserObject();
      initializeTool(tool);
    }
	}
	
	private void initializeTool(Class<? extends CommandLineTool> tool) {
		// Clear the configuration panel
		configurationPanel.removeAll();
		
		// Attempt to instantiate the tool and extract parameter information
		CommandLineTool app = null;
		try {
			app = tool.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.error("Error initializing tool: " + tool.getSimpleName());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
			    "Error initializing tool",
			    "Tool Error",
			    JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		JCommander jc = new JCommander(app);
		List<ParameterDescription> parameters = jc.getParameters();
		for (ParameterDescription paramDescription : parameters) {
			JPanel fieldPanel = new JPanel();
			fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.LINE_AXIS));
			configurationPanel.add(fieldPanel);
			
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
				Assembly[] assemblies = new Assembly[manager.getAvailableAssemblies().size()];
				assemblies = manager.getAvailableAssemblies().toArray(assemblies);
				final JComboBox<Assembly> cbAssemblyChooser = new JComboBox<Assembly>(assemblies);
				cbAssemblyChooser.setSelectedItem(lastUsedAssembly);
				cbAssemblyChooser.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						lastUsedAssembly = (Assembly) cbAssemblyChooser.getSelectedItem();
					}
				});
				fieldPanel.add(cbAssemblyChooser);
			} else {
				JTextField textField = new JTextField();
				fieldPanel.add(textField);
			}
		}
		
		// Set the help text
		StringBuilder helpText = new StringBuilder();
		jc.usage(helpText);
		helpTextPanel.setText(helpText.toString());
		
		configurationPanel.revalidate();
	}
	
	private void runTool() {
		if (currentTool == null) return;
		
		// Validate the required parameters
		log.debug("Validating parameters for tool");
		
		// Run the tool
		log.info("Running tool");
		progressBar.setIndeterminate(true);
		try {
			CommandLineTool app = currentTool.newInstance();
			app.instanceMain(null);
		} catch (InstantiationException | IllegalAccessException e) {
			log.error("Error while running tool");
			e.printStackTrace();
		}
		
		// Set the progress bar
		log.info("Tool completed successfully");
		progressBar.setIndeterminate(false);
		progressBar.setValue(100);
		
		// Capture output
	}
	
	public void handleAbout(ApplicationEvent event) {
		JOptionPane.showMessageDialog(this, "Java Genomics Toolkit v1.0");
		if (event != null) {
			event.setHandled(true);
		}
	}

	public void handleOpenApplication(ApplicationEvent event) {
		// Application was opened
	}

	public void handleOpenFile(ApplicationEvent event) {
		//JOptionPane.showMessageDialog(frmToolRunner, "OS X told us to open " + event.getFilename());
	}

	public void handlePreferences(ApplicationEvent event) {
		//JOptionPane.showMessageDialog(frmToolRunner, "No preferences available");
	}

	public void handlePrintFile(ApplicationEvent event) {
		//JOptionPane.showMessageDialog(frmToolRunner, "OS X told us to print " + event.getFilename());
	}

	public void handleQuit(ApplicationEvent event) {
		dispose();
		System.exit(0);
	}

	public void handleReOpenApplication(ApplicationEvent event) {
		//JOptionPane.showMessageDialog(frmToolRunner, "OS X told the application was reopened");
	}
}
