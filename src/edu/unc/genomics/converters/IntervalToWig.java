package edu.unc.genomics.converters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.Assembly;
import edu.unc.genomics.Interval;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.ValuedInterval;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileWriter;

/**
 * Convert interval-based data such as microarray data in Bed, BedGraph, or GFF format
 * to Wig format. Overlapping probes in the original interval dataset are averaged.
 * 
 * @author timpalpant
 *
 */
public class IntervalToWig extends CommandLineTool {

	private static final Logger log = Logger.getLogger(IntervalToWig.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (Bed/BedGraph/GFF)", required = true)
	public Path intervalFile;
	@Parameter(names = {"-z", "--zero"}, description = "Assume zero where there is no data (default = NaN)")
	public boolean defaultZero = false;
	@Parameter(names = {"-a", "--assembly"}, description = "Genome assembly", required = true)
	public Assembly assembly;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wig)", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		log.debug("Initializing input/output files");
		TrackHeader header = TrackHeader.newWiggle();
		header.setName("Converted " + intervalFile.getFileName());
		header.setDescription("Converted " + intervalFile.getFileName());
		try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(intervalFile);
				 WigFileWriter writer = new WigFileWriter(outputFile, header)) {			
			// Process each chromosome in the assembly
			for (String chr : reader.chromosomes()) {
				if (!assembly.includes(chr)) {
					log.info("Skipping chromosome "+chr+" because it is not in assembly "+assembly);
					continue;
				}
				
				log.debug("Processing chromosome " + chr);
				int chunkStart = 1;
				while (chunkStart < assembly.getChrLength(chr)) {
					int chunkStop = Math.min(chunkStart+DEFAULT_CHUNK_SIZE-1, assembly.getChrLength(chr));
					Interval chunk = new Interval(chr, chunkStart, chunkStop);
					float[] sum = new float[chunk.length()];
					int[] count = new int[chunk.length()];
					
					Iterator<? extends Interval> it = reader.query(chunk);
					while (it.hasNext()) {
						ValuedInterval entry = (ValuedInterval) it.next();
						if (entry.getValue() != null) {
							int entryStart = Math.max(chunkStart, entry.low());
							int entryStop = Math.min(chunkStop, entry.high());
							for (int i = entryStart; i <= entryStop; i++) {
								sum[i-chunkStart] += entry.getValue().floatValue();
								count[i-chunkStart]++;
							}
						}
					}
					
					// Calculate the average at each base pair in the chunk
					for (int i = 0; i < sum.length; i++) {
						if (count[i] == 0 && !defaultZero) {
							sum[i] = Float.NaN;
						} else {
							sum[i] /= count[i];
						}
					}
					
					// Process the next chunk
					chunkStart = chunkStop + 1;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		new IntervalToWig().instanceMain(args);
	}

}
