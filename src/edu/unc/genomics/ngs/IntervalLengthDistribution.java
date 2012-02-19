package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.math.stat.Frequency;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;

public class IntervalLengthDistribution extends CommandLineTool {

	private static final Logger log = Logger.getLogger(IntervalLengthDistribution.class);

	@Parameter(names = {"-i", "--input"}, description = "Interval file", required = true)
	public IntervalFile<? extends Interval> inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	
	
	@Override
	public void run() throws IOException {
		log.debug("Generating histogram of interval lengths");
		Frequency freq = new Frequency();
		int min = Integer.MAX_VALUE;
		int max = -1;
		for (Interval i : inputFile) {
			int L = i.length();
			freq.addValue(L);
			
			if (L < min) {
				min = L;
			}
			if (L > max) {
				max = L;
			}
		}
		
		log.debug("Writing histogram output");
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			for (int i = min; i <= max; i++) {
				writer.write(i+"\t"+freq.getCount(i));
				writer.newLine();
			}
		}
		
		inputFile.close();
	}
	
	public static void main(String[] args) {
		new IntervalLengthDistribution().instanceMain(args);
	}

}
