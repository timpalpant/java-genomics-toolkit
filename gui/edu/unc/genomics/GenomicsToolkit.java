package edu.unc.genomics;

import java.awt.EventQueue;

import javax.swing.JFrame;

/**
 * @author timpalpant
 * The main application for running the genomics toolkit gui
 */
public class GenomicsToolkit {

	private JFrame frmToolRunner;

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

	/**
	 * Create the application.
	 */
	public GenomicsToolkit() {
		frmToolRunner = new ToolRunnerFrame();
	}
}
