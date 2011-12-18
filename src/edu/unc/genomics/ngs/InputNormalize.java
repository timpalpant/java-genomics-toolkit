package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;

public class InputNormalize extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(InputNormalize.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads)", required = true)
	public IntervalFile<? extends Interval> inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (read length histogram)", required = true)
	public Path outputFile;
		
	public void run() throws IOException {		
		log.debug("Initializing input file");
		
	}
	
	public static void main(String[] args) throws IOException {
		InputNormalize a = new InputNormalize();
		JCommander jc = new JCommander(a);
		jc.setProgramName(InputNormalize.class.getSimpleName());
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			jc.usage();
			System.exit(-1);
		}
		
		a.run();
	}
}