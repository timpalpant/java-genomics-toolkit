package edu.unc.genomics;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

/**
 * Controller for scheduling and running jobs
 * Wrapper for ExcecutorService, although the implementation could change
 * 
 * @author timpalpant
 *
 */
public class JobQueueManager {
		
	private static final Logger log = Logger.getLogger(JobQueueManager.class);
	
	private final JobQueue queue;
	private final ExecutorService exec;
	private final Thread monitor;
	
	public JobQueueManager(JobQueue queue) {
		this.queue = queue;
		
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
		
		// Add the SubmittedJob to the JobQueue
		queue.add(submittedJob);
		return submittedJob;
	}
	
	/**
	 * Are any jobs running? (not done)
	 * @return
	 */
	public boolean isRunning() {
		for (SubmittedJob job : queue) {
			if (!job.isDone()) {
				return true;
			}
		}
		
		return false;
	}


	/**
	 * Background process for polling the status of submitted jobs
	 * @author timpalpant
	 *
	 */
	public class JobMonitor implements Runnable {

		public static final int JOB_POLL_INTERVAL = 1_000;
		
		public void run() {
			try {
				while (true) {
					// Check Job statuses every 1s
					Thread.sleep(JOB_POLL_INTERVAL);
					
					for (SubmittedJob job : queue) {
						if (job.isDone()) {
							queue.update(job);
						}
					}
				}
			} catch (InterruptedException e) {
				log.fatal("JobMonitor crashed");
				e.printStackTrace();
				throw new RuntimeException("JobMonitor crashed");
			}
		}
	}

}
