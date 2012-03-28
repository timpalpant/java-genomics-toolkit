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
import edu.unc.genomics.Interval;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.ValuedInterval;
import edu.unc.genomics.io.IntervalFile;

public class IntervalToWig extends CommandLineTool {

	private static final Logger log = Logger.getLogger(IntervalToWig.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (Bed/BedGraph)", required = true)
	public IntervalFile<? extends Interval> intervalFile;
	@Parameter(names = {"-z", "--zero"}, description = "Assume zero where there is no data (default = NaN)")
	public boolean defaultZero = false;
	@Parameter(names = {"-a", "--assembly"}, description = "Genome assembly", required = true)
	public Assembly assembly;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wig)", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		log.debug("Initializing output file");
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wiggle track header to the output file
			TrackHeader header = new TrackHeader("wiggle_0");
			header.setName("Converted " + intervalFile.getPath().getFileName());
			header.setDescription("Converted " + intervalFile.getPath().getFileName());
			writer.write(header.toString());
			writer.newLine();
			
			// Process each chromosome in the assembly
			for (String chr : intervalFile.chromosomes()) {
				log.debug("Processing chromosome " + chr);
				// Write the contig header to the output file
				writer.write("fixedStep chrom="+chr+" start=1 step=1 span=1");
				writer.newLine();
				
				int start = 1;
				while (start < assembly.getChrLength(chr)) {
					int stop = start + DEFAULT_CHUNK_SIZE - 1;
					int length = stop - start + 1;
					int[] count = new int[length];
					float[] sum = new float[length];
					
					Iterator<? extends Interval> it = intervalFile.query(chr, start, stop);
					while (it.hasNext()) {
						ValuedInterval entry = (ValuedInterval) it.next();
						if (entry.getValue() != null) {
							int entryStart = Math.max(start, entry.getStart());
							int entryStop = Math.min(stop, entry.getStop());
							for (int i = entryStart; i <= entryStop; i++) {
								sum[i-start] += entry.getValue().floatValue();
								count[i-start]++;
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
							writer.write(String.valueOf(sum[i]/count[i]));
						}
						writer.newLine();
					}
					
					// Process the next chunk
					start = stop + 1;
				}
			}
		} finally {
			intervalFile.close();
		}
	}
	
	public static void main(String[] args) {
		new IntervalToWig().instanceMain(args);
	}

}
