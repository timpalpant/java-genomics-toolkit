package edu.unc.genomics;

import java.util.ArrayList;
import java.util.List;

public class JobQueue {
	
	private final List<Job> jobs = new ArrayList<>();
	
	/**
	 * Add a Job to the queue
	 * @param job
	 * @throws JobException 
	 */
	public void addJob(Job job) throws JobException {
		// Refuse to add the Job to the queue if its arguments are not valid
		if (!job.validateArguments()) {
			throw new JobException("Job arguments are not valid!");
		}
		
		jobs.add(job);
	}
}
