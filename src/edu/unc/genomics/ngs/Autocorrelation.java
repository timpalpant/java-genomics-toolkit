package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.PositiveIntegerValidator;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class Autocorrelation extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(Autocorrelation.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile wig;
	@Parameter(names = {"-l", "--loci"}, description = "Genomic loci (Bed format)", required = true)
	public IntervalFile<? extends Interval> loci;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	@Parameter(names = {"-m", "--max"}, description = "Autocorrelation limit (bp)", validateWith = PositiveIntegerValidator.class)
	public int limit = 200;
	
	private void abs2(float[] data) {
		for (int i = 0; i < data.length; i+=2) {
			data[i] = data[i]*data[i] + data[i+1]*data[i+1];
			data[i+1] = 0;
		}
	}
	
	@Override
	public void run() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			log.debug("Computing autocorrelation for each window");
			int skipped = 0;
			for (Interval interval : loci) {
				if (interval.length() < limit) {
					log.debug("Skipping interval: " + interval.toString());
					skipped++;
					continue;
				}
				
				Iterator<WigItem> wigIter;
				try {
					wigIter = wig.query(interval);
				} catch (IOException | WigFileException e) {
					log.debug("Skipping interval: " + interval.toString());
					skipped++;
					continue;
				}
				
				float[] data = WigFile.flattenData(wigIter, interval.getStart(), interval.getStop());
				
				// Compute the autocorrelation with the Wiener-Khinchin theorem
				FloatFFT_1D fft = new FloatFFT_1D(data.length);
				fft.realForward(data);
				abs2(data);
				fft.realInverse(data, true);
	
				writer.write(StringUtils.join(data, "\t"));
				writer.newLine();
			}
			
			log.info("Skipped " + skipped + " intervals");
		}
	}
	
	public static void main(String[] args) {
		new Autocorrelation().instanceMain(args);
	}
}