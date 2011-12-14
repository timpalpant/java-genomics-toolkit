package edu.unc.genomics;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

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
	private final JProgressBar progressBar = new JProgressBar();
	private final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
	private final JobConfigPanel configurationPanel = new JobConfigPanel();
	private final JTextPane helpTextPanel = new JTextPane();
	private final ToolsTree toolsTree = new ToolsTree();
		
	private final JobQueueManager queueManager = new JobQueueManager();
	private final JList<SubmittedJob> queueList = new JList<>(queueManager.getListModel());

	/**
	 * Create the frame.
	 */
	public ToolRunnerFrame() {
    //application.addPreferencesMenuItem();
    //application.setEnabledPreferencesMenu(true);
    application.addApplicationListener(this);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Genomics Toolkit Tool Runner");
		setBounds(100, 100, 1000, 600);
		
		contentPane.setBorder(BorderFactory.createEmptyBorder());
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		initializeChildren();
		initializeMenuBar();
	}
	
	private void initializeChildren() {
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		initializeQueuePanel();
		initializeToolsTree();
		
		splitPane.setRightComponent(tabbedPane);
		
		initializeConfigurationPanel();
		initializeHelpPanel();
		initializeRunPanel();
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
		queuePanel.setBorder(new EmptyBorder(5, 0, 0, 0));
		queuePanel.setLayout(new BoxLayout(queuePanel, BoxLayout.PAGE_AXIS));
		contentPane.add(queuePanel, BorderLayout.EAST);
		
		JLabel queueLabel = new JLabel("Job Queue");
		queueLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		queuePanel.add(queueLabel);
		
		queueList.setPreferredSize(new Dimension(200, 0));
		queuePanel.add(queueList);
	}
	
	private void initializeConfigurationPanel() {
		JScrollPane configScrollPane = new JScrollPane(configurationPanel);
		tabbedPane.addTab("Tool Configuration", null, configScrollPane, "Configure tool");
		configurationPanel.setLayout(new BoxLayout(configurationPanel, BoxLayout.PAGE_AXIS));
		configurationPanel.setBackground(tabbedPane.getBackground());
	}
	
	private void initializeHelpPanel() {
		JPanel helpPanel = new JPanel();
		tabbedPane.addTab("Help", null, helpPanel, null);
		helpPanel.setLayout(new BorderLayout(0, 0));
		
		helpTextPanel.setEditable(false);
		helpTextPanel.setBackground(tabbedPane.getBackground());
		JScrollPane helpScrollPane = new JScrollPane(helpTextPanel);
		helpPanel.add(helpScrollPane);
	}
	
	private void initializeRunPanel() {
		JPanel runPanel = new JPanel();
		runPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
		contentPane.add(runPanel, BorderLayout.SOUTH);
		runPanel.setLayout(new BoxLayout(runPanel, BoxLayout.X_AXIS));
		
		runPanel.add(progressBar);
		
		JButton btnRun = new JButton("Run");
		btnRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addJobToQueue();
			}
		});
		runPanel.add(btnRun);
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
				Job job = new Job(tool);
	      configurationPanel.setJob(job);
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
		dispose();
		System.exit(0);
	}

	public void handleReOpenApplication(ApplicationEvent event) {
		//JOptionPane.showMessageDialog(frmToolRunner, "OS X told the application was reopened");
	}
}
