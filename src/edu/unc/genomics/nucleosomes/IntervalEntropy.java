package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * For each interval, compute the mean/min/max/total/coverage of data in a (Big)Wig file over that interval.
 * @author timpalpant
 *
 */
public class IntervalEntropy extends CommandLineTool {

	private static final Logger log = Logger.getLogger(IntervalStats.class);

	@Parameter(description = "Input files", required = true)
	public List<String> inputFiles = new ArrayList<>();
	@Parameter(names = {"-l", "--loci"}, description = "Loci file (Bed)", 
             required = true, validateWith = ReadablePathValidator.class)
	public Path lociFile;
	@Parameter(names = {"-w", "--window"}, description = "Moving window size")
	public int window = 147;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	
	private List<WigFileReader> wigs = new ArrayList<>();
	
	@Override
	public void run() throws IOException {
		log.debug("Initializing input Wig file(s)");
		for (String inputFile : inputFiles) {
			wigs.add(WigFileReader.autodetect(Paths.get(inputFile)));
		}
		
		log.debug("Initializing output file");
		int count = 0, skipped = 0;
		try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(lociFile);
				 BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			writer.write("#Chr\tStart\tStop\tID\tAlignment\tStrand");
			for (String inputFile : inputFiles) {
				Path p = Paths.get(inputFile);
				writer.write("\t" + p.getFileName().toString());
			}
			writer.newLine();
			
			//log.debug("Iterating over all intervals and computing "+stat);
			for (Interval interval : reader) {
				writer.write(interval.toBed());
				
				for (WigFileReader wig : wigs) {
					try {
						SummaryStatistics result = wig.queryStats(interval);
						float value = Float.NaN;
						writer.write("\t" + value);
					} catch (WigFileException e) {
						writer.write("\t" + Float.NaN);
						skipped++;
					}
				}
				
				writer.newLine();
				count++;
			}
		}
		
		for (WigFileReader wig : wigs) {
			wig.close();
		}
		
		log.info(count + " intervals processed");
		log.info(skipped + " interval skipped");
	}
	
	public static void main(String[] args) {
		new IntervalEntropy().instanceMain(args);
	}

}
