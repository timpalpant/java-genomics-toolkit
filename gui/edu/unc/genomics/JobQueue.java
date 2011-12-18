package edu.unc.genomics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.apache.log4j.Logger;

/**
 * Model for the queue of SubmittedJobs
 * Should be managed through the JobQueueManager controller
 * 
 * @author timpalpant
 *
 */
public class JobQueue implements ListModel<SubmittedJob>, Iterable<SubmittedJob> {
	
	private static final Logger log = Logger.getLogger(JobQueue.class);

	private final List<SubmittedJob> submittedJobs = new ArrayList<>();
	private final List<ListDataListener> dataListeners = new ArrayList<>();
	
	public void add(SubmittedJob job) {
		int N = submittedJobs.size();
		submittedJobs.add(job);
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, N, N);
		for (ListDataListener l : dataListeners) {
			l.intervalAdded(e);
		}
	}
	
	public void remove(SubmittedJob job) {
		submittedJobs.remove(job);
		int N = submittedJobs.size();
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, N, N);
		for (ListDataListener l : dataListeners) {
			l.intervalAdded(e);
		}
	}
	
	public void update(SubmittedJob job) {
		int index = submittedJobs.indexOf(job);
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, index);
		for (ListDataListener l : dataListeners) {
			l.intervalAdded(e);
		}
	}
	
	@Override
	public int getSize() {
		return submittedJobs.size();
	}

	@Override
	public SubmittedJob getElementAt(int index) {
		return submittedJobs.get(index);
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		dataListeners.add(l);		
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		dataListeners.remove(l);
	}

	@Override
	public Iterator<SubmittedJob> iterator() {
		return submittedJobs.iterator();
	}
}
