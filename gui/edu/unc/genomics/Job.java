package edu.unc.genomics;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;

/**
 * A Job represents an instance of a tool configured with specific arguments
 * Attempting to run a Job with invalid arguments will throw a RuntimeException
 * If tool execution fails, a RuntimeException will also be thrown
 * UncheckedExceptions should be managed by setting the Job's UncheckedExceptionManager
 * or providing a ThreadGroup / default UncheckedExceptionManager
 * 
 * @author timpalpant
 *
 */
public class Job implements Iterable<ParameterDescription>, Runnable {
	
	private static final Logger log = Logger.getLogger(Job.class);
	
	private final Class<? extends CommandLineTool> tool;
	private final CommandLineTool app;
	private final List<ParameterDescription> parameters;
	private final String usageText;
	private boolean isRunning = false;
	//private StringWriter writer = new StringWriter();
	
	/**
	 * Arguments for running this Job
	 */
	private Map<ParameterDescription,String> args = new HashMap<>();
	
	/**
	 * Creates a new Job model for the specified tool
	 * @param tool
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public Job(final Class<? extends CommandLineTool> tool) throws InstantiationException, IllegalAccessException {
		this.tool = tool;
		
		// Attempt to instantiate the tool and extract parameter information
		app = tool.newInstance();
		JCommander jc = new JCommander(app);
		jc.setProgramName(tool.getSimpleName());
		parameters = jc.getParameters();
		StringBuilder sbuilder = new StringBuilder();
		jc.usage(sbuilder);
		usageText = sbuilder.toString();
		
		// Set default arguments
		for (ParameterDescription param : parameters) {
			if (param.getDefault() != null) {
				setArgument(param, String.valueOf(param.getDefault()));
			}
		}
	}
	
	/**
	 * Copy-constructor
	 * @param job
	 */
	public Job(final Job job) {
		this.tool = job.tool;
		this.app = job.app;
		this.parameters = job.parameters;
		this.args = job.args;
		this.usageText = job.usageText;
	}
	
	@Override
	public void run() {
		// Load the arguments for running the tool
		String[] args;
		try {
			args = getArguments();
		} catch (JobException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new CommandLineToolException("Job arguments are not valid");
		}
		
		// Attempt to instantiate and run the tool
		//Appender appender = new WriterAppender(new PatternLayout(), writer);
		//Logger.getRootLogger().addAppender(appender);
		isRunning = true;
		app.instanceMain(args);
		isRunning = false;
		//Logger.getRootLogger().removeAppender(appender);
	}
	
	/**
	 * Render the arguments for running an instance of this Job
	 * @return
	 * @throws JobException 
	 */
	public String[] getArguments() throws JobException {
		if (!validateArguments()) {
			throw new JobException("Job Arguments are not valid");
		}
		
		List<String> cmdArgs = new ArrayList<>();
		for (ParameterDescription p : args.keySet()) {
			cmdArgs.add(p.getLongestName());
			cmdArgs.add(args.get(p));
		}
		
		String[] ret = new String[cmdArgs.size()];
		return cmdArgs.toArray(ret);
	}
	
	public String getArgument(final ParameterDescription p) {
		return args.get(p);
	}
	
	/**
	 * Set a value for the given parameter
	 * @param p
	 * @param value
	 */
	public void setArgument(final ParameterDescription p, final String value) {
		if (value.length() == 0) {
			args.remove(p);
		} else {
			args.put(p, value);
		}
	}
	
	/**
	 * Remove all set arguments for this Job
	 */
	public void resetArguments() {
		args.clear();
	}
	
	/**
	 * Is this parameter set?
	 * @param p
	 * @return
	 */
	public boolean isSet(final ParameterDescription p) {
		return args.containsKey(p);
	}
	
	/**
	 * Validate that this job has all of its parameters set
	 * and that they are all valid
	 * @return
	 */
	public boolean validateArguments() {
		// TODO: Better validation based on parameter type
		boolean hasAllRequiredParams = true;
		for (ParameterDescription param : parameters) {
			if (param.getParameter().required() && !isSet(param)) {
				log.debug("Job is missing required argument: " + param.getLongestName());
				hasAllRequiredParams = false;
			}
		}
		
		return hasAllRequiredParams;
	}
	
	public int numParameters() {
		return parameters.size();
	}

	@Override
	public Iterator<ParameterDescription> iterator() {
		return parameters.iterator();
	}
	
	public String getName() {
		return tool.getSimpleName();
	}
	
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * @return the usageText
	 */
	public String getUsageText() {
		return usageText;
	}

	@Override
	public String toString() {
		return getName();
	}
	
}
