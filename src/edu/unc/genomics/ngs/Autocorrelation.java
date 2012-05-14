package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.FFTUtils;

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

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile wig;
	@Parameter(names = {"-l", "--loci"}, description = "Genomic loci (Bed format)", required = true)
	public IntervalFile<? extends Interval> loci;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	@Parameter(names = {"-m", "--max"}, description = "Autocorrelation limit (bp)")
	public int limit = 200;
	
	@Override
	public void run() throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			log.debug("Computing autocorrelation for each window");
			int skipped = 0;
			for (Interval interval : loci) {				
				Iterator<WigItem> wigIter;
				try {
					wigIter = wig.query(interval);
				} catch (IOException | WigFileException e) {
					log.debug("Skipping interval: " + interval.toString());
					skipped++;
					continue;
				}
				
				// Compute the autocorrelation
				float[] data = WigFile.flattenData(wigIter, interval.getStart(), interval.getStop());
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