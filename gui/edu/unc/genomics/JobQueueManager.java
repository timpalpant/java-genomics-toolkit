package edu.unc.genomics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.DefaultListModel;

import org.apache.log4j.Logger;

/**
 * Controller for scheduling and running jobs
 * Wrapper for ExcecutorService, although the implementation could change
 * 
 * @author timpalpant
 *
 */
public class JobQueueManager {
	
	public static final int JOB_POLL_INTERVAL = 1000;
	
	private static final Logger log = Logger.getLogger(JobQueueManager.class);
	
	private final ExecutorService exec;
	private final Thread monitor;
	private final DefaultListModel<SubmittedJob> listModel;
	private final List<SubmittedJob> submittedJobs = new ArrayList<>();
	
	public JobQueueManager() {
		listModel = new DefaultListModel<>();
		
		int numProcessors = Runtime.getRuntime().availableProcessors();
		log.debug("Initializing thread pool with "+numProcessors+" processors");
		exec = Executors.newFixedThreadPool(numProcessors);
		monitor = new Thread(new JobMonitor());
		monitor.start();
	}
	
	public List<Runnable> shutdownNow() {
		return exec.shutdownNow();
	}
	
	/**
	 * Add a Job to the queue
	 * @param job
	 * @throws JobException 
	 */
	public SubmittedJob submitJob(Job job) throws JobException {
		// Refuse to add the Job to the queue if its arguments are not valid
		if (!job.validateArguments()) {
			throw new JobException("Job arguments are not valid");
		}
		
		// Submit the job for execution into the thread pool
		Future<?> future = exec.submit(job);
		SubmittedJob submittedJob = new SubmittedJob(job, future);
		log.info("Submitted job " + submittedJob.getId());
		
		// Add the Job to the ListModel
		submittedJobs.add(submittedJob);
		listModel.addElement(submittedJob);

		return submittedJob;
	}

	/**
	 * @return the listModel
	 */
	public DefaultListModel<SubmittedJob> getListModel() {
		return listModel;
	}
	
	private class JobMonitor implements Runnable {
		public void run() {
			try {
				while (true) {
					// Check Job statuses every 1s
					Thread.sleep(JOB_POLL_INTERVAL);
					
					for (SubmittedJob job : submittedJobs) {
						//if (job.getFuture().)
						if (job.getFuture().isDone()) {
							listModel.removeElement(job);
						}
					}
				}
			} catch (InterruptedException e) {
				// Don't care
			}
		}
	}
}
