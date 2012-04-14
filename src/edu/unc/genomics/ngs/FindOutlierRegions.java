package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.wigmath.WigMathTool;

public class FindOutlierRegions extends CommandLineTool {

	private static final Logger log = Logger.getLogger(FindOutlierRegions.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-w", "--window"}, description = "Window size", required = true)
	public int windowSize;
	@Parameter(names = {"-t", "--threshold"}, description = "Threshold (x mean)")
	public float threshold = 3;
	@Parameter(names = {"-o", "--output"}, description = "Output file (bed)", required = true)
	public Path outputFile;
	
	double mean;
	DescriptiveStatistics stats;

	@Override
	public void run() throws IOException {
		mean = inputFile.mean();
		
		stats = new DescriptiveStatistics();
		stats.setWindowSize(windowSize);
		
		// Run through the genome finding regions that exceed the threshold
	}
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new FindOutlierRegions().instanceMain(args);
	}

}
