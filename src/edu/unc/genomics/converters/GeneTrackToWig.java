package edu.unc.genomics.converters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.Assembly;
import edu.unc.genomics.Contig;
import edu.unc.genomics.GeneTrackEntry;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.GeneTrackFileReader;
import edu.unc.genomics.io.WigFileWriter;

import edu.unc.genomics.ReadablePathValidator;

/**
 * Convert a GeneTrack format file to Wig, optionally shifting and merging the +/- strands
 * @author timpalpant
 *
 */
public class GeneTrackToWig extends CommandLineTool {

	private static final Logger log = Logger.getLogger(GeneTrackToWig.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (GeneTrack)", required = true, validateWith = ReadablePathValidator.class)
	public Path gtFile;
	@Parameter(names = {"-z", "--zero"}, description = "Assume zero where there is no data (default = NaN)")
	public boolean defaultZero = false;
	@Parameter(names = {"-s", "--shift"}, description = "Shift from 5' end (bp)")
	public Integer shift;
	@Parameter(names = {"-a", "--assembly"}, description = "Genome assembly", required = true)
	public Assembly assembly;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wig)", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		log.debug("Initializing input/output files");
		TrackHeader header = TrackHeader.newWiggle();
		header.setName("Converted " + gtFile.getFileName());
		header.setDescription("Converted " + gtFile.getFileName());
		try (GeneTrackFileReader gt = new GeneTrackFileReader(gtFile);
				 WigFileWriter writer = new WigFileWriter(outputFile, header)) {			
			// Process each chromosome in the assembly
			for (String chr : gt.chromosomes()) {
				if (!assembly.includes(chr)) {
					log.info("Skipping chromosome "+chr+" because it is not in assembly "+assembly);
					continue;
				}
				
				log.debug("Processing chromosome " + chr);
				int chunkStart = 1;
				while (chunkStart < assembly.getChrLength(chr)) {
					int chunkStop = Math.min(chunkStart + DEFAULT_CHUNK_SIZE - 1, assembly.getChrLength(chr));
					Interval chunk = new Interval(chr, chunkStart, chunkStop);
					int[] count = new int[chunk.length()];
					float[] sum = new float[chunk.length()];
					
					// Pad to shift length
					int paddedStart = chunkStart;
					int paddedStop = chunkStop;
					if (shift != null) {
						paddedStart = Math.max(chunkStart-shift-1, 1);
						paddedStop = Math.min(chunkStop+shift+1, assembly.getChrLength(chr));
					}
					Iterator<GeneTrackEntry> it = gt.query(chr, paddedStart, paddedStop);
					while (it.hasNext()) {
						GeneTrackEntry entry = it.next();
						int entryPos = entry.getStart();
						if (shift == null || shift == 0) {
							sum[entryPos-chunkStart] += entry.getValue().floatValue();
							count[entryPos-chunkStart]++;
						} else {
							if (entry.getForward() > 0) {
								int forwardShift = entryPos + shift;
								if (forwardShift >= chunkStart && forwardShift-chunkStart < sum.length) {
									sum[forwardShift-chunkStart] += entry.getForward();
									count[forwardShift-chunkStart]++;
								}
							}
							
							if (entry.getReverse() > 0) {
								int reverseShift = entryPos - shift;
								if (reverseShift >= chunkStart && reverseShift-chunkStart < sum.length) {
									sum[reverseShift-chunkStart] += entry.getReverse();
									count[reverseShift-chunkStart]++;
								}
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
					
					// Write the chunk to disk
					writer.write(new Contig(chunk, sum));
					
					// Process the next chunk
					chunkStart = chunkStop + 1;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		new GeneTrackToWig().instanceMain(args);
	}

}
