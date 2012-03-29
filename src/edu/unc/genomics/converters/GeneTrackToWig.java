package edu.unc.genomics.converters;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.Assembly;
import edu.unc.genomics.GeneTrackEntry;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.io.GeneTrackFile;

public class GeneTrackToWig extends CommandLineTool {

	private static final Logger log = Logger.getLogger(GeneTrackToWig.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (GeneTrack)", required = true)
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
		log.debug("Initializing input GeneTrack file");
		GeneTrackFile gt = new GeneTrackFile(gtFile);
		
		log.debug("Initializing output file");
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wiggle track header to the output file
			TrackHeader header = new TrackHeader("wiggle_0");
			header.setName("Converted " + gtFile.getFileName());
			header.setDescription("Converted " + gtFile.getFileName());
			writer.write(header.toString());
			writer.newLine();
			
			// Process each chromosome in the assembly
			for (String chr : gt.chromosomes()) {
				if (!assembly.includes(chr)) {
					log.info("Skipping chromosome "+chr+" because it is not in assembly "+assembly);
					continue;
				}
				
				log.debug("Processing chromosome " + chr);
				// Write the contig header to the output file
				writer.write("fixedStep chrom="+chr+" start=1 step=1 span=1");
				writer.newLine();
				
				int start = 1;
				while (start < assembly.getChrLength(chr)) {
					int stop = Math.min(start + DEFAULT_CHUNK_SIZE - 1, assembly.getChrLength(chr));
					int length = stop - start + 1;
					int[] count = new int[length];
					float[] sum = new float[length];
					
					// Pad to shift length
					int paddedStart = start;
					int paddedStop = stop;
					if (shift != null) {
						paddedStart = Math.max(start-shift-1, 1);
						paddedStop = Math.min(stop+shift+1, assembly.getChrLength(chr));
					}
					Iterator<GeneTrackEntry> it = gt.query(chr, paddedStart, paddedStop);
					while (it.hasNext()) {
						GeneTrackEntry entry = it.next();
						int entryPos = entry.getStart();
						if (shift == null || shift == 0) {
							sum[entryPos-start] += entry.getValue().floatValue();
							count[entryPos-start]++;
						} else {
							if (entry.getForward() > 0) {
								int forwardShift = entryPos + shift;
								if (forwardShift >= start && forwardShift-start < sum.length) {
									sum[forwardShift-start] += entry.getForward();
									count[forwardShift-start]++;
								}
							}
							
							if (entry.getReverse() > 0) {
								int reverseShift = entryPos - shift;
								if (reverseShift >= start && reverseShift-start < sum.length) {
									sum[reverseShift-start] += entry.getReverse();
									count[reverseShift-start]++;
								}
							}
						}
					}
					
					// Write the average at each base pair to the output file
					for (int i = 0; i < sum.length; i++) {
						if (count[i] == 0) {
							if (defaultZero) {
								writer.write("0");
							} else {
								writer.write(String.valueOf(Float.NaN));
							}
						} else {
							writer.write(String.valueOf(sum[i]));
						}
						writer.newLine();
					}
					
					// Process the next chunk
					start = stop + 1;
				}
			}
		} finally {
			gt.close();
		}
	}
	
	public static void main(String[] args) {
		new GeneTrackToWig().instanceMain(args);
	}

}
