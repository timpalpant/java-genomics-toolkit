package edu.unc.genomics;

import javax.swing.JList;

/**
 * List view for the JobQueue
 * @author timpalpant
 *
 */
public class JobQueueList extends JList<Job> {

	private static final long serialVersionUID = -7925162962043233583L;

	private final JobQueue queue;
	
	public JobQueueList(JobQueue queue) {
		this.queue = queue;
	}
}
