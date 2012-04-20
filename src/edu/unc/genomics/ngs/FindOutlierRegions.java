package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class FindOutlierRegions extends CommandLineTool {

	private static final Logger log = Logger.getLogger(FindOutlierRegions.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-w", "--window"}, description = "Window size", required = true)
	public int windowSize;
	@Parameter(names = {"-t", "--threshold"}, description = "Threshold (fold x mean)")
	public float fold = 3;
	@Parameter(names = {"-o", "--output"}, description = "Output file (bed)", required = true)
	public Path outputFile;
	
	double threshold;
	DescriptiveStatistics stats;

	@Override
	public void run() throws IOException {
		threshold = fold * inputFile.mean();
		
		stats = new DescriptiveStatistics();
		stats.setWindowSize(windowSize);
		
		// Run through the genome finding regions that exceed the threshold
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			for (String chr : inputFile.chromosomes()) {
				int start = inputFile.getChrStart(chr);
				int stop = inputFile.getChrStop(chr);
				log.debug("Processing chromosome " + chr + " region " + start + "-" + stop);
				
				// Process the chromosome in chunks
				int bp = start;
				Integer outlierStart = null;
				while (bp < stop) {
					int chunkStart = bp;
					int chunkStop = Math.min(bp+DEFAULT_CHUNK_SIZE-1, stop);
					log.debug("Processing chunk "+chr+":"+chunkStart+"-"+chunkStop);
					
					try {
						Iterator<WigItem> result = inputFile.query(chr, chunkStart, chunkStop);
						float[] data = WigFile.flattenData(result, chunkStart, chunkStop);
						for (int i = 0; i < data.length; i++) {
							stats.addValue(data[i]);
							
							// If the mean of the current window is > threshold
							// write it to output as a potential outlier region
							if (outlierStart == null) {
								// Start a new outlier region
								if (stats.getMean() > threshold) {
									outlierStart = bp + i - windowSize;
								}
							} else {
								// End an outlier region
								if (stats.getMean() < threshold) {
									int outlierStop = bp + i;
									writer.write(chr+"\t"+outlierStart+"\t"+outlierStop);
									writer.newLine();
									outlierStart = null;
								}
							}
						}
					} catch (WigFileException e) {
						log.fatal("Wig file error while processing chunk " + chr + " region " + start + "-" + stop);
						e.printStackTrace();
						throw new RuntimeException("Wig file error while processing chunk " + chr + " region " + start + "-" + stop);
					}
					
					bp = chunkStop + 1;
				}
			}
		}
		
		
		inputFile.close();
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
