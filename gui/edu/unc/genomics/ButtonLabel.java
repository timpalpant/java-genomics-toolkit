package edu.unc.genomics;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 * Act like a button, look like a label
 * 
 * @author timpalpant
 *
 */
public class ButtonLabel extends JButton {

	private static final long serialVersionUID = -4449260534784095223L;

	public ButtonLabel() {
		init();
	}

	public ButtonLabel(Icon icon) {
		super(icon);
		init();
	}

	public ButtonLabel(String text) {
		super(text);
		init();
	}

	public ButtonLabel(Action a) {
		super(a);
		init();
	}

	public ButtonLabel(String text, Icon icon) {
		super(text, icon);
		init();
	}
	
	private void init() {
		setBorder(BorderFactory.createEmptyBorder());
		setBorderPainted(false);  
		setContentAreaFilled(false);  
		setFocusPainted(false);  
		setOpaque(false);
	}

}
