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

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.FFTUtils;

public class PowerSpectrum extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(PowerSpectrum.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (Wig)", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Genomic loci (Bed format)", required = true)
	public IntervalFile<? extends Interval> loci;
	@Parameter(names = {"-m", "--max"}, description = "Only output this many frequencies")
	public int max = 40;
	@Parameter(names = {"-o", "--output"}, description = "Output file (tabular)", required = true)
	public Path outputFile;
		
	public void run() throws IOException {		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write header
			writer.write("#chr\tlow\thigh\tid\talignment\tstrand\tPower Spectrum Values");
			writer.newLine();
					
			log.debug("Computing power spectrum for each window");
			int skipped = 0;
			for (Interval interval : loci) {
				Iterator<WigItem> wigIter;
				try {
					wigIter = inputFile.query(interval);
				} catch (IOException | WigFileException e) {
					log.debug("Skipping interval: " + interval.toString());
					skipped++;
					continue;
				}
				
				if (interval.length() > 1) {
					float[] data = WigFile.flattenData(wigIter, interval.getStart(), interval.getStop());
					// Compute the power spectrum
					FloatFFT_1D fft = new FloatFFT_1D(data.length);
					fft.realForward(data);
					float[] ps = FFTUtils.abs2(data);
					// and normalize the power spectrum
					float sum = 0;
					for (int i = 1; i < ps.length; i++) {
						sum += ps[i];
					}
					for (int i = 1; i < ps.length; i++) {
						ps[i] /= sum;
					}
		
					writer.write(interval.toBed());
					for (int i = 1; i < Math.min(ps.length, max); i++) {
						writer.write("\t"+ps[i]);
					}
					writer.newLine();
				} else {
					skipped++;
					writer.write(interval.toBed());
					writer.newLine();
				}
			}
			
			log.info("Skipped " + skipped + " intervals");
		}		
	}
	
	public static void main(String[] args) {
		new PowerSpectrum().instanceMain(args);
	}
}