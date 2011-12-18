package edu.unc.genomics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.simplericity.macify.eawt.Application;
import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;
import org.simplericity.macify.eawt.DefaultApplication;
import org.xml.sax.SAXException;

import com.beust.jcommander.JCommander;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * The main ToolRunner window
 * and controller for creating, running, and managing Jobs
 * 
 * @author timpalpant
 *
 */
public class ToolRunnerFrame extends JFrame implements ApplicationListener {
	
	private static final long serialVersionUID = 6454774196137357898L;
	private static final Logger log = Logger.getLogger(ToolRunnerFrame.class);
	
	private final Application application = new DefaultApplication();
	
	private final JPanel contentPane = new JPanel();
	private final JSplitPane splitPane = new JSplitPane();
	private final JPanel mainPane = new JPanel();
	private final JProgressBar progressBar = new JProgressBar();
	private final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
	private final JobConfigPanel configurationPanel = new JobConfigPanel();
	private final JTextPane helpTextPanel = new JTextPane();
	private final ToolsTree toolsTree = new ToolsTree();
		
	private final JobQueue queue = new JobQueue();
	private final JobQueueManager queueManager = new JobQueueManager(queue);
	private final JList<SubmittedJob> queueList = new JList<>(queue);

	/**
	 * Create the frame.
	 */
	public ToolRunnerFrame() {
    //application.addPreferencesMenuItem();
    //application.setEnabledPreferencesMenu(true);
    application.addApplicationListener(this);
		
    // set OS X-specific properties
    if (application.isMac()) {
    	setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    	//toolsTree.putClientProperty("Quaqua.Tree.style", "sourceList");
    } else {
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
		setTitle("Genomics Toolkit Tool Runner");
		setBounds(100, 100, 1000, 600);
		
		contentPane.setBorder(BorderFactory.createEmptyBorder());
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		initializeChildren();
		initializeMenuBar();
	}
	
	private void initializeChildren() {
		splitPane.setBorder(BorderFactory.createEmptyBorder());
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		initializeQueuePanel();
		initializeToolsTree();
		
		mainPane.setLayout(new BorderLayout(0, 0));
		mainPane.setBorder(BorderFactory.createEmptyBorder());
		mainPane.add(tabbedPane, BorderLayout.CENTER);
		
		JPanel runPanel = new JPanel();
		runPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		runPanel.setLayout(new BoxLayout(runPanel, BoxLayout.X_AXIS));
		runPanel.add(progressBar);
		JButton btnRun = new JButton("Run");
		btnRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addJobToQueue();
			}
		});
		runPanel.add(btnRun);
		mainPane.add(runPanel, BorderLayout.SOUTH);
		splitPane.setRightComponent(mainPane);
		
		initializeConfigurationPanel();
		initializeHelpPanel();
	}
	
	private void initializeMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFileMenu = new JMenu("File");
		menuBar.add(mnFileMenu);
		
		JMenuItem mntmAssemblyManager = new JMenuItem("Assembly manager");
		mntmAssemblyManager.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JMenuItem menuItem = (JMenuItem) e.getSource();  
        JPopupMenu popupMenu = (JPopupMenu) menuItem.getParent();  
        Component invoker = popupMenu.getInvoker(); //this is the JMenu (in my code)  
        JComponent invokerAsJComponent = (JComponent) invoker;  
        Container topLevel = invokerAsJComponent.getTopLevelAncestor();
				AssemblyManagerDialog dialog = new AssemblyManagerDialog((JFrame) topLevel);
				//dialog.getRootPane().putClientProperty("Window.style", "small");
				dialog.setVisible(true);
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
	
	private void initializeQueuePanel() {
		JPanel queuePanel = new JPanel();
		queuePanel.setLayout(new BoxLayout(queuePanel, BoxLayout.PAGE_AXIS));
		queuePanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY));
		contentPane.add(queuePanel, BorderLayout.EAST);
		
		JLabel queueLabel = new JLabel("Job Queue");
		queueLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		queueLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		queuePanel.add(queueLabel);

		queueList.setBackground(contentPane.getBackground());
		queueList.setCellRenderer(new JobQueueCellRenderer());
		JScrollPane queueListScrollPane = new JScrollPane(queueList);
		queueListScrollPane.setBorder(BorderFactory.createEmptyBorder());
		queueListScrollPane.setBackground(contentPane.getBackground());
		queueListScrollPane.setPreferredSize(new Dimension(200, Integer.MAX_VALUE));
		queuePanel.add(queueListScrollPane);
	}
	
	private void initializeConfigurationPanel() {		
		JScrollPane configScrollPane = new JScrollPane(configurationPanel);
		configScrollPane.setBorder(BorderFactory.createEmptyBorder());
		tabbedPane.addTab("Tool Configuration", null, configScrollPane, "Configure tool");
	}
	
	private void initializeHelpPanel() {
		JPanel helpPanel = new JPanel();
		tabbedPane.addTab("Help", null, helpPanel, null);
		helpPanel.setLayout(new BorderLayout(0, 0));
		
		helpTextPanel.setEditable(false);
		helpTextPanel.setBackground(tabbedPane.getBackground());
		Font mono = new Font("Monospaced", helpTextPanel.getFont().getStyle(), helpTextPanel.getFont().getSize());
		helpTextPanel.setFont(mono);
		JScrollPane helpScrollPane = new JScrollPane(helpTextPanel);
		helpScrollPane.setBorder(BorderFactory.createEmptyBorder());
		helpPanel.add(helpScrollPane);
	}
		
	private void initializeToolsTree() {
		try {
			ToolsTreeModel model = ToolsTreeModel.loadDefaultConfig();
			toolsTree.setModel(model);
		} catch (ParserConfigurationException | SAXException | IOException e1) {
			log.error("Error loading tool configuration file");
			e1.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			log.error("Error loading tool: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}

		toolsTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				changeTool();
			}
		});
		
		JScrollPane toolsTreeScrollPane = new JScrollPane(toolsTree);
		//toolsTreeScrollPane.setBorder(BorderFactory.createEmptyBorder());
		splitPane.setLeftComponent(toolsTreeScrollPane);
	}

	/**
	 * Change the configuration panel to the currently selected tool
	 * to configure a new Job
	 */
	private void changeTool() {
		// Returns the last path element of the selection.
    Object node = toolsTree.getLastSelectedPathComponent();
    // Nothing is selected
    if (node == null) {   
    	return;
    }

    if (node instanceof ToolsTreeNode) {
    	ToolsTreeNode toolNode = (ToolsTreeNode) node;
			try {
	      Class<? extends CommandLineTool> tool = toolNode.getClazz();
	      
	      // Set up the configuration panel to configure this tool
				Job job = new Job(tool);
	      configurationPanel.setJob(job);
	      // Set the help text to the usage
	      helpTextPanel.setText(job.getUsageText());
			} catch (InstantiationException | IllegalAccessException e) {
				log.error("Error initializing Job");
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error initializing job", "Job Initialization Error", JOptionPane.ERROR_MESSAGE);
			}

    }
	}
	
	private void addJobToQueue() {
		Job currentJob = configurationPanel.getJob();
		if (currentJob == null) return;
		
		// Validate the required parameters
		log.info("Validating parameters for tool");
		if (!currentJob.validateArguments()) {
			configurationPanel.highlightInvalidArguments();
			return;
		}
		
		// Add the job to the queue
		try {
			queueManager.submitJob(currentJob);
			configurationPanel.setJob(null);
		} catch (JobException e) {
			log.error("Error adding Job to queue");
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error adding job to queue", "Job Queue Error", JOptionPane.ERROR_MESSAGE);
		}
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
		boolean confirm = true;
		if (queueManager.isRunning()) {
			int result = JOptionPane.showConfirmDialog(this, "Jobs are currently running. Are you sure you want to quit?", "Confirm Quit", JOptionPane.OK_CANCEL_OPTION);
			confirm = (result == JOptionPane.OK_OPTION);
		}
		
		if (confirm) {
			dispose();
			System.exit(0);
		}
	}

	public void handleReOpenApplication(ApplicationEvent event) {
		//JOptionPane.showMessageDialog(frmToolRunner, "OS X told the application was reopened");
		setVisible(true);
	}
}
