package edu.unc.genomics;

import java.awt.EventQueue;

import javax.swing.JFrame;

/**
 * The main application for running the genomics toolkit gui
 * Could do resource checking, etc. prior to startup
 * 
 * @author timpalpant
 * 
 */
public class GenomicsToolkit {

	private JFrame frmToolRunner = new ToolRunnerFrame();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					GenomicsToolkit window = new GenomicsToolkit();
					window.frmToolRunner.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
