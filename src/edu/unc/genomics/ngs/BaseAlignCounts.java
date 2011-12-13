package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;

public class BaseAlignCounts extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(BaseAlignCounts.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads)", required = true)
	public IntervalFile<? extends Interval> inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (read length histogram)", required = true)
	public Path outputFile;
		
	public void run() throws IOException {
		
	}
	
	public static void main(String[] args) throws IOException {
		new BaseAlignCounts().instanceMain(args);
	}
}