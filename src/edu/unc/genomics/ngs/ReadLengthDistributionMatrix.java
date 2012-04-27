package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;

public class ReadLengthDistributionMatrix extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(ReadLengthDistributionMatrix.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads)", required = true)
	public IntervalFile<? extends Interval> intervalFile;
	@Parameter(names = {"-c", "--chr"}, description = "Chromosome", required = true)
	public String chr;
	@Parameter(names = {"-s", "--start"}, description = "Start base pair", required = true)
	public int start;
	@Parameter(names = {"-e", "--stop"}, description = "Stop base pair", required = true)
	public int stop;
	@Parameter(names = {"-m", "--min"}, description = "Minimum fragment length bin (bp)")
	public int min = 1;
	@Parameter(names = {"-l", "--max"}, description = "Maximum fragment length bin (bp)")
	public int max = 200;
	@Parameter(names = {"-b", "--bin"}, description = "Bin size (bp)")
	public int binSize = 1;
	@Parameter(names = {"-o", "--output"}, description = "Output file (tabular)", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		int regionLength = stop - start + 1;
		int lengthRange = max - min + 1;
		int histLength = lengthRange / binSize;
		if (histLength*binSize != lengthRange) {
			histLength++;
		}
		
		log.debug("Binning reads by genomic location and length");
		int[][] counts = new int[histLength][regionLength];
		Iterator<? extends Interval> reads = intervalFile.query(chr, start, stop);
		int skipped = 0;
		while (reads.hasNext()) {
			Interval read = reads.next();
			if (read.length() < min || read.length() > max) {
				skipped++;
				continue;
			}
			int bin = (read.length() - min) / binSize;
			int intersectStart = Math.max(read.getStart(), start);
			int intersectStop = Math.min(read.getStop(), stop);
			for (int i = intersectStart; i <= intersectStop; i++) {
				counts[bin][i-start]++;
			}
		}
		
		log.info("Skipped "+skipped+" reads with length outside range");
		
		// Write to output in matrix2png format
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Header line
			writer.write(chr);
			for (int bp = start; bp <= stop; bp++) {
				writer.write("\t"+bp);
			}
			
			for (int i = histLength-1; i >= 0; i--) {
				writer.newLine();
				writer.write(String.valueOf(min+i*binSize));
				for (int j = 0; j < regionLength; j++) {
					writer.write("\t"+counts[i][j]);
				}
			}
		}
		
		intervalFile.close();
	}
	
	public static void main(String[] args) {
		new ReadLengthDistributionMatrix().instanceMain(args);
	}
}