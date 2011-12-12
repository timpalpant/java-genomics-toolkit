package edu.unc.genomics.ngs;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.CommandLineTool;

public class BaseAlignCounts extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(BaseAlignCounts.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads)", required = true)
	public String inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (read length histogram)", required = true)
	public String outputFile;
		
	public void run() throws IOException {		
		log.debug("Initializing input file");
		
	}
	
	public static void main(String[] args) throws IOException {
		BaseAlignCounts a = new BaseAlignCounts();
		JCommander jc = new JCommander(a);
		jc.setProgramName(BaseAlignCounts.class.getSimpleName());
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			jc.usage();
			System.exit(-1);
		}
		
		a.run();
	}
}