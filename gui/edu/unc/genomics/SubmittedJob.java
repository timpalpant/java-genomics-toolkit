package edu.unc.genomics;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Represents a job that was submitted for processing
 * 
 * @author timpalpant
 *
 */
public class SubmittedJob {
	private static int numJobs = 0;
	
	private final Future<?> future;
	private final int id;
	private final Job job;
	
	public SubmittedJob(Job job, Future<?> future) {
		this.id = ++numJobs;
		this.job = job;
		this.future = future;
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the job
	 */
	public Job getJob() {
		return job;
	}
	
	/**
	 * If the job is currently running
	 * @return
	 */
	public boolean isRunning() {
		return job.isRunning() && !isDone();
	}
	
	/**
	 * If the job is done running
	 * (it may have failed or succeeded)
	 * @return
	 */
	public boolean isDone() {
		return future.isDone();
	}
	
	/**
	 * If this job completed without any Exceptions
	 * @return
	 */
	public boolean succeeded() {
		return (future.isDone() && !failed());
	}
	
	/**
	 * If this job completed with Exceptions
	 * @return
	 */
	public boolean failed() {
		if (future.isDone()) {
			try {
				future.get();
				return false;
			} catch (InterruptedException | ExecutionException e) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Return an Exception that occured, or null if there were none
	 * or the job is not yet done
	 * @return
	 */
	public Exception getException() {
		if (future.isDone()) {
			try {
				future.get();
				return null;
			} catch (InterruptedException | ExecutionException e) {
				return e;
			}
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return "Job "+id+": "+job.getName();
	}
}
