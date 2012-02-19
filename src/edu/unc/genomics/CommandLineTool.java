package edu.unc.genomics;

import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * A command-line script
 * @author timpalpant
 *
 */
public abstract class CommandLineTool {
	
	/**
	 * JCommander command-line argument parser
	 */
	private final JCommander jc = new JCommander(this);
	
	public CommandLineTool() {
		// Add factories for parsing Paths, Assemblies, IntervalFiles, and WigFiles
		jc.addConverterFactory(new PathFactory());
		jc.addConverterFactory(new AssemblyFactory());
		jc.addConverterFactory(new IntervalFileFactory());
		jc.addConverterFactory(new WigFileFactory());
		
		// Set the program name to be the class name
		jc.setProgramName(this.getClass().getSimpleName());
	}
	
	/**
	 * The default bite-size to use for applications that process files in chunks
	 * TODO Read from a configuration file
	 */
	public static final int DEFAULT_CHUNK_SIZE = 300_000;
	
	/**
	 * Do the main computation of this tool
	 * @throws IOException
	 */
	public abstract void run() throws IOException;
	
	/**
	 * Parse command-line arguments and run the tool
	 * Exit on parameter exceptions
	 * @param args
	 */
	public void instanceMain(String[] args) throws CommandLineToolException {
		try {
			toolRunnerMain(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			jc.usage();
			System.exit(-1);
		}
	}
	
	/**
	 * Parse command-line arguments and run the tool
	 * @param args
	 * @throws ParameterException if there are invalid/missing parameters
	 * @throws CommandLineToolException if an exception occurs while running the tool
	 */
	public void toolRunnerMain(String[] args) throws ParameterException, CommandLineToolException {
		jc.parse(args);
		
		try {
			run();
		} catch (IOException e) {
			e.printStackTrace();
			throw new CommandLineToolException("IO error while running tool");
		}
	}
}
