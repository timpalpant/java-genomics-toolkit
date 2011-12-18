package edu.unc.genomics;

import java.awt.EventQueue;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

/**
 * The main application for running the genomics toolkit gui
 * Could do resource checking, etc. prior to startup
 * 
 * @author timpalpant
 * 
 */
public class ToolRunner {
	
	private static final Logger log = Logger.getLogger(ToolRunner.class);

	private JFrame frmToolRunner = new ToolRunnerFrame();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					ToolRunner window = new ToolRunner();
					window.frmToolRunner.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
