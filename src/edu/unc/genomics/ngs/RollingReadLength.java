package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.Assembly;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileWriter;

/**
 * Creates a new Wig file with the mean read length of reads covering each base pair.
 * @author timpalpant
 *
 */
public class RollingReadLength extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(RollingReadLength.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads)", required = true, validateWith = ReadablePathValidator.class)
	public Path intervalFile;
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
			// Process each chromosome that has reads and is in the assembly
			for (String chr : reader.chromosomes()) {
				if (!assembly.includes(chr)) {
					continue;
				}
				
				log.debug("Processing chromosome " + chr);
				int start = 1;
				while (start < assembly.getChrLength(chr)) {
					int stop = Math.min(start+DEFAULT_CHUNK_SIZE-1, assembly.getChrLength(chr));
					Interval chunk = new Interval(chr, start, stop);
					int[] sum = new int[chunk.length()];
					int[] count = new int[chunk.length()];
					
					Iterator<? extends Interval> it = reader.query(chunk);
					while (it.hasNext()) {
						Interval entry = it.next();
						int entryStart = Math.max(entry.low(), start);
						int entryStop = Math.min(entry.high(), stop);
						for (int i = entryStart; i <= entryStop; i++) {
							sum[i-start] += entry.length();
							count[i-start]++;
						}
					}
					
					// Calculate the average at each base pair
					float[] avg = new float[chunk.length()];
					for (int i = 0; i < avg.length; i++) {
						if (count[i] == 0) {
							avg[i] = Float.NaN;
						} else {
							avg[i] = ((float)sum[i]) / count[i];
						}
					}
					
					// Write this chunk to disk
					writer.write(new Contig(chunk, avg));
					
					// Process the next chunk
					start = stop + 1;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		new RollingReadLength().instanceMain(args);
	}
}