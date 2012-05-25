package edu.unc.genomics.ngs;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Finds regions of a Wig file that differ significantly from the mean, such as CNVs or deletions.
 * @author timpalpant
 *
 */
public class FindOutlierRegions extends CommandLineTool {

	private static final Logger log = Logger.getLogger(FindOutlierRegions.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-w", "--window"}, description = "Window size", required = true)
	public int windowSize;
	@Parameter(names = {"-t", "--threshold"}, description = "Threshold (fold x mean)")
	public float fold = 3;
	@Parameter(names = {"-b", "--below"}, description = "Search for outliers below the threshold")
	public boolean below = false;
	@Parameter(names = {"-o", "--output"}, description = "Output file (bed)", required = true)
	public Path outputFile;
	
	int flip = 1;
	double threshold;
	DescriptiveStatistics stats = new DescriptiveStatistics();

	@Override
	public void run() throws IOException {
		stats.setWindowSize(windowSize);
		if (below) {
			flip = -1;
		}
		
		// Run through the genome finding regions that exceed the threshold
		try (WigFileReader reader = WigFileReader.autodetect(inputFile);
				 PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, Charset.defaultCharset()))) {
			threshold = fold * reader.mean();
			
			for (String chr : reader.chromosomes()) {
				int start = reader.getChrStart(chr);
				int stop = reader.getChrStop(chr);
				log.debug("Processing chromosome "+chr+" region "+start+"-"+stop);
				
				// Process the chromosome in chunks
				int bp = start;
				Integer outlierStart = null;
				stats.clear();
				while (bp < stop) {
					int chunkStart = bp;
					int chunkStop = Math.min(bp+DEFAULT_CHUNK_SIZE-1, stop);
					Interval chunk = new Interval(chr, chunkStart, chunkStop);
					log.debug("Processing chunk "+chunk);
					
					try {
						float[] data = reader.query(chunk).getValues();
						for (int i = 0; i < data.length; i++) {
							stats.addValue(data[i]);
							
							// If the mean of the current window is > threshold
							// write it to output as a potential outlier region
							if (outlierStart == null) {
								// Start a new outlier region
								if (flip*stats.getMean() > flip*threshold) {
									outlierStart = bp + i - windowSize;
								}
							} else {
								// End an outlier region
								if (flip*stats.getMean() < flip*threshold) {
									int outlierStop = bp + i;
									writer.println(chr+"\t"+outlierStart+"\t"+outlierStop);
									outlierStart = null;
								}
							}
						}
					} catch (WigFileException e) {
						log.fatal("Wig file error while processing chunk "+chr+":"+start+"-"+stop);
						e.printStackTrace();
						throw new RuntimeException("Wig file error while processing chunk "+chr+":"+start+"-"+stop);
					}
					
					bp = chunkStop + 1;
				}
			}
		}
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
