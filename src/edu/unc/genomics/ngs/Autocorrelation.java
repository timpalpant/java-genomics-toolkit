package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.io.WigQueryResult;
import edu.unc.utils.FFTUtils;

import edu.unc.genomics.ReadablePathValidator;

/**
 * <p>This tool computes the unnormalized autocovariance of intervals of data in a Wig file.</p>
 * 
 * <p>Syntax<br/>
 * <b>Input data:</b> is the genomic data on which to compute the autocorrelation.<br/>
 * <b>List of intervals:</b> The autocorrelation will be computed for each genomic interval specified in this list.<br/>
 * <b>Maximum shift:</b> In computing the autocorrelation, the data will be phase-shifted up to this limit.<br/>
 * </p>
 *
 * <p>For more information, see: <a href="http://en.wikipedia.org/wiki/Autocorrelation">Wikipedia</a></p>
 *
 * @author timpalpant
 *
 */
public class Autocorrelation extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(Autocorrelation.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Genomic loci (Bed format)", required = true)
	public IntervalFileReader<? extends Interval> loci;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	@Parameter(names = {"-m", "--max"}, description = "Autocorrelation limit (bp)")
	public int limit = 200;
	
	@Override
	public void run() throws IOException {
		try (WigFileReader wig = WigFileReader.autodetect(inputFile);
				BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			log.debug("Computing autocorrelation for each window");
			int skipped = 0;
			for (Interval interval : loci) {				
				WigQueryResult result;
				try {
					result = wig.query(interval);
				} catch (IOException | WigFileException e) {
					log.debug("Skipping interval: " + interval.toString());
					skipped++;
					continue;
				}
				
				// Compute the autocorrelation
				float[] data = result.getValues();
				float[] auto = FFTUtils.autocovariance(data, limit);
	
				// Write to output
				writer.write(interval.toBed());
				for (int i = 0; i < auto.length; i++) {
					writer.write("\t" + auto[i]);
				}
				writer.newLine();
			}
			
			log.info("Skipped " + skipped + " intervals");
		}
	}
	
	public static void main(String[] args) {
		new Autocorrelation().instanceMain(args);
	}
}