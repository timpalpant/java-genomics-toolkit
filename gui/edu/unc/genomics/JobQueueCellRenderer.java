package edu.unc.genomics;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

/**
 * @author timpalpant
 *
 */
public class JobQueueCellRenderer extends JPanel implements ListCellRenderer<SubmittedJob> {

	private static final long serialVersionUID = 4270263302075586018L;
	
	private static final ImageIcon inProgressIcon = new ImageIcon(ResourceManager.getImagesDirectory().resolve("beachball.png").toString());
	private static final ImageIcon rerunIcon = new ImageIcon(ResourceManager.getImagesDirectory().resolve("arrow-circle.png").toString());
	private static final ImageIcon infoIcon = new ImageIcon(ResourceManager.getImagesDirectory().resolve("information-white.png").toString());
	private static final ImageIcon logIcon = new ImageIcon(ResourceManager.getImagesDirectory().resolve("sticky-note-text.png").toString());
	private static final ImageIcon showFileIcon = new ImageIcon(ResourceManager.getImagesDirectory().resolve("eye_icon.png").toString());
	
	private static final ImageIcon successIcon = new ImageIcon(ResourceManager.getImagesDirectory().resolve("icon_success_sml.gif").toString());
	private static final ImageIcon errorIcon = new ImageIcon(ResourceManager.getImagesDirectory().resolve("icon_error_sml.gif").toString());
	private static final ImageIcon warningIcon = new ImageIcon(ResourceManager.getImagesDirectory().resolve("icon_warning_sml.gif").toString());

	private JLabel statusIconLabel = new JLabel(inProgressIcon);
	private JLabel nameLabel = new JLabel();
	
	public JobQueueCellRenderer() {
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEADING, 5, 2);
		setPreferredSize(new Dimension(190, 48));
		setLayout(flowLayout);
		setBorder(BorderFactory.createLineBorder(Color.GRAY, 1, true));
		
		JPanel statusPanel = new JPanel();
		statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 16, 0));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.PAGE_AXIS));
		statusPanel.add(statusIconLabel);
		add(statusPanel);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setAlignmentX(LEFT_ALIGNMENT);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		
		JPanel namePanel = new JPanel();
		namePanel.setLayout(flowLayout);
		nameLabel.setPreferredSize(new Dimension(145, 16));
		nameLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
		namePanel.add(nameLabel);
		mainPanel.add(namePanel);
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(flowLayout);
		mainPanel.add(buttonsPanel);
		
		ButtonLabel rerunLabel = new ButtonLabel(rerunIcon);
		rerunLabel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//addJobToQueue();
			}
		});
		buttonsPanel.add(rerunLabel);
		ButtonLabel infoLabel = new ButtonLabel(infoIcon);
		infoLabel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//addJobToQueue();
			}
		});
		buttonsPanel.add(infoLabel);
		ButtonLabel logLabel = new ButtonLabel(logIcon);
		logLabel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//addJobToQueue();
			}
		});
		buttonsPanel.add(logLabel);
		ButtonLabel showFileLabel = new ButtonLabel(showFileIcon);
		showFileLabel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//addJobToQueue();
			}
		});
		buttonsPanel.add(showFileLabel);
		
		add(mainPanel);
	}

	@Override
	public Component getListCellRendererComponent(
			JList<? extends SubmittedJob> list, SubmittedJob value, int index,
			boolean isSelected, boolean cellHasFocus) {
		
		nameLabel.setText(value.toString());
		if (value.isRunning()) {
			statusIconLabel.setIcon(inProgressIcon);
			nameLabel.setForeground(Color.BLACK);
		} else if (value.succeeded()) {
			statusIconLabel.setIcon(successIcon);
			nameLabel.setForeground(Color.BLACK);
		} else {
			statusIconLabel.setIcon(errorIcon);
			nameLabel.setForeground(Color.RED);
		}
		
		return this;
	}

}
