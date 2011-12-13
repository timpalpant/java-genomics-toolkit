package edu.unc.genomics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;

public class Job implements Iterable<ParameterDescription> {
	
	private static final Logger log = Logger.getLogger(Job.class);
	
	private final Class<? extends CommandLineTool> tool;
	private final List<ParameterDescription> parameters;
	
	/**
	 * Arguments for running this Job
	 */
	private Map<String,String> args = new HashMap<>();
	
	/**
	 * Creates a new Job model for the specified tool
	 * @param tool
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public Job(final Class<? extends CommandLineTool> tool) throws InstantiationException, IllegalAccessException {
		this.tool = tool;
		
		// Attempt to instantiate the tool and extract parameter information
		CommandLineTool app = tool.newInstance();
		JCommander jc = new JCommander(app);
		parameters = jc.getParameters();
	}
	
	/**
	 * Render the arguments for running an instance of this Job
	 * @return
	 */
	public String[] getArguments() {
		List<String> cmdArgs = new ArrayList<>();
		for (String name : args.keySet()) {
			cmdArgs.add(name);
			cmdArgs.add(args.get(name));
		}
		
		String[] ret = new String[cmdArgs.size()];
		return cmdArgs.toArray(ret);
	}
	
	/**
	 * Set a value for the given parameter
	 * @param p
	 * @param value
	 */
	public void setArgument(final ParameterDescription p, final String value) {
		args.put(p.getLongestName(), value);
	}
	
	/**
	 * Remove all set arguments for this Job
	 */
	public void resetArguments() {
		args.clear();
	}
	
	/**
	 * Validate that this job has all of its parameters set
	 * and that they are all valid
	 * @return
	 */
	public boolean validateArguments() {
		// TODO: Better validation based on parameter type
		for (ParameterDescription param : parameters) {
			if (param.getParameter().required() && !args.containsKey(param.getLongestName())) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Iterator<ParameterDescription> iterator() {
		return parameters.iterator();
	}
}
