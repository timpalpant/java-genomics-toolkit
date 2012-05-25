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
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileWriter;

/**
 * This tool calculates the coverage of sequencing reads (or any interval data)
 * and creates a new Wig file with the number of reads overlapping each base pair.
 * 
 * @author timpalpant
 *
 */
public class BaseAlignCounts extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(BaseAlignCounts.class);
		
	@Parameter(names = {"-i", "--input"}, description = "Input file (mapped reads)", required = true)
	public Path intervalFile;
	@Parameter(names = {"-a", "--assembly"}, description = "Genome assembly", required = true)
	public Assembly assembly;
	@Parameter(names = {"-x", "--extend"}, description = "Extend reads from 5' end (default = read length)")
	public Integer extend = -1;
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
					float[] count = new float[chunk.length()];
					log.debug("Processing chunk "+chunk);
					
					Iterator<? extends Interval> it = reader.query(chunk);
					while (it.hasNext()) {
						Interval entry = it.next();
						int entryStop = entry.getStop();
						if (extend != null && extend != -1) {
							if (entry.isWatson()) {
								entryStop = entry.getStart() + extend - 1;
							} else {
								entryStop = entry.getStart() - extend + 1;
							}
						}
						
						// Clamp to the current chunk
						int low = Math.max(Math.min(entry.getStart(), entryStop), chunkStart);
						int high = Math.min(Math.max(entry.getStart(), entryStop), chunkStop);
						for (int i = low; i <= high; i++) {
							count[i-chunkStart]++;
						}
					}
					
					// Write the count at each base pair to the output file
					writer.write(new Contig(chunk, count));
					
					// Process the next chunk
					chunkStart = chunkStop + 1;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		new BaseAlignCounts().instanceMain(args);
	}
}