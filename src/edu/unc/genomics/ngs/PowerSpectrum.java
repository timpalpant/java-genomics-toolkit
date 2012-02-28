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

public class PowerSpectrum extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(PowerSpectrum.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (Wig)", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Genomic loci (Bed format)", required = true)
	public IntervalFile<? extends Interval> loci;
	@Parameter(names = {"-o", "--output"}, description = "Output file (tabular)", required = true)
	public Path outputFile;
		
	/**
	 * Computes the power spectrum from FFT data
	 * taking into accound even/odd length arrays
	 * refer to JTransforms documentation for layout of the FFT data
	 * @param f
	 * @return
	 */
	private float[] abs2(float[] f) {
		int n = f.length;
		float[] ps = new float[n/2+1];
		// DC component
		ps[0] = (f[0]*f[0]) / (n*n); 
		
		// Even
		if (n % 2 == 0) {
			for (int k = 1; k < n/2; k++) {
				ps[k] = f[2*k]*f[2*k] + f[2*k+1]*f[2*k+1];
			}
			ps[n/2] = f[1]*f[1];
		// Odd
		} else {
			for (int k = 1; k < (n-1)/2; k++) {
				ps[k] = f[2*k]*f[2*k] + f[2*k+1]*f[2*k+1];
			}
			
			ps[(n-1)/2] = f[n-1]*f[n-1] + f[1]*f[1];
		}
		
		return ps;
	}
	
	public void run() throws IOException {		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
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
					float[] ps = abs2(data);
					// and normalize the power spectrum
					float sum = 0;
					for (int i = 1; i < ps.length; i++) {
						sum += ps[i];
					}
					for (int i = 1; i < ps.length; i++) {
						ps[i] /= sum;
					}
		
					writer.write(interval.toBed());
					for (int i = 1; i < Math.min(ps.length, 40); i++) {
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