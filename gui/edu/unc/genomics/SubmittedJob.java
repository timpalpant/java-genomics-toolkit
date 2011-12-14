package edu.unc.genomics;

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
		this.id = numJobs++;
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
	 * @return the future
	 */
	public Future<?> getFuture() {
		return future;
	}
	
	@Override
	public String toString() {
		return "Job "+id;
	}
}
