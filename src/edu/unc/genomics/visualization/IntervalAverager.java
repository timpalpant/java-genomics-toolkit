package edu.unc.genomics.visualization;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.BedEntry;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.BedFile;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class IntervalAverager extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(IntervalAverager.class);
	
	@Parameter(names = {"-i", "--input"}, description = "Input file (Wig)", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Loci file (Bed)", required = true, validateWith = ReadablePathValidator.class)
	public Path lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (matrix2png format)", required = true)
	public Path outputFile;
	
	private List<BedEntry> loci;
	
	@Override
	public void run() throws IOException {		
		log.debug("Loading alignment intervals");
		try (BedFile bed = new BedFile(lociFile)) {
			loci = bed.loadAll();
		}
		
		// Compute the matrix dimensions
		int leftMax = Integer.MIN_VALUE;
		int rightMax = Integer.MIN_VALUE;
		for (BedEntry entry : loci) {
			if (entry.getValue() == null) {
				throw new CommandLineToolException("You must specify an alignment point for each interval in column 5");
			}
			int left = Math.abs(entry.getValue().intValue()-entry.getStart());
			int right = Math.abs(entry.getValue().intValue()-entry.getStop());
			if (left > leftMax) {
				leftMax = left;
			}
			if (right > rightMax) {
				rightMax = right;
			}
		}
		
		int m = loci.size();
		int n = leftMax + rightMax + 1;
		int alignmentPoint = leftMax;
		log.info("Intervals aligned into: " + m+"x"+n + " matrix");
		log.info("Alignment point: " + alignmentPoint);
				
		float[] sum = new float[n];
		int[] counts = new int[n];
		int count = 0, skipped = 0;	
		log.debug("Iterating over all intervals");
		for (BedEntry entry : loci) {
			Iterator<WigItem> result = null;
			try {
				result = inputFile.query(entry);
			} catch (WigFileException e) {
				skipped++;
				continue;
			}
			
			float[] data = WigFile.flattenData(result, entry.getStart(), entry.getStop());
			
			// Locus alignment point (entry value) should be positioned over the global alignment point
			int n1 = alignmentPoint - Math.abs(entry.getValue().intValue()-entry.getStart());
			int n2 = alignmentPoint + Math.abs(entry.getValue().intValue()-entry.getStop());
			assert data.length == n2-n1+1;
			for (int bp = n1; bp <= n2; bp++) {
				sum[bp] += data[bp-n1];
				counts[bp]++;
			}
			
			count++;
		}
		
		inputFile.close();
		log.info(count + " intervals processed");
		log.info(skipped + " intervals skipped");
		
		log.debug("Computing average");
		float[] avg = new float[n];
		for (int i = 0; i < n; i++) {
			if (counts[i] == 0) {
				avg[i] = Float.NaN;
			} else {
				avg[i] = sum[i] / counts[i];
			}
		}
		
		log.debug("Writing average to output");
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			for (int i = 0; i < n; i++) {
				writer.write(i-alignmentPoint + "\t" + avg[i]);
				writer.newLine();				
			}
		}
	}
	
	public static void main(String[] args) {
		new IntervalAverager().instanceMain(args);
	}
}