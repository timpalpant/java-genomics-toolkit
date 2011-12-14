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
	 * The default bite-size to use for applications that process files in chunks
	 */
	public static final int DEFAULT_CHUNK_SIZE = 500_000;
	
	/**
	 * Do the main computation of this tool
	 * @throws IOException
	 */
	public abstract void run() throws IOException;
	
	/**
	 * Initialize parameters
	 * @param args
	 */
	public void parseArguments(String[] args, boolean exitOnMissingRequired) {
		JCommander jc = new JCommander(this);
		
		// Add factories for parsing Paths, Assemblies, IntervalFiles, and WigFiles
		jc.addConverterFactory(new PathFactory());
		jc.addConverterFactory(new AssemblyFactory());
		jc.addConverterFactory(new IntervalFileFactory());
		jc.addConverterFactory(new WigFileFactory());
		
		// Set the program name to be the class name
		jc.setProgramName(this.getClass().getSimpleName());
		
		// Attempt to parse the arguments and exit with usage if there is an error
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			jc.usage();
			if (exitOnMissingRequired) {
				System.exit(1);
			}
		}
	}
	
	/**
	 * Parse command-line arguments and run the tool
	 * @param args
	 */
	public void instanceMain(String[] args) {
		parseArguments(args, true);
		
		try {
			run();
		} catch (IOException e) {
			e.printStackTrace();
			throw new CommandLineToolException("IO error while running tool");
		}
	}
}
