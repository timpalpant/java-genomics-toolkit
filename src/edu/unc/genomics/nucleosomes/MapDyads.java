package edu.unc.genomics.nucleosomes;

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
 * Count the number of read centers overlapping each base pair in the genome
 * @author timpalpant
 *
 */
public class MapDyads extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(MapDyads.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads)", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-s", "--size"}, description = "Mononucleosome length (default: read length)")
	public Integer nucleosomeSize;
	@Parameter(names = {"-a", "--assembly"}, description = "Genome assembly", required = true)
	public Assembly assembly;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wig)", required = true)
	public Path outputFile;
		
	@Override
	public void run() throws IOException {		
		log.debug("Initializing output file");
		int mapped = 0;
		TrackHeader header = TrackHeader.newWiggle();
		header.setName("Converted " + inputFile.getFileName());
		header.setDescription("Converted " + inputFile.getFileName());
		try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(inputFile);
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
					
					Iterator<? extends Interval> it = reader.query(chunk);
					while (it.hasNext()) {
						Interval entry = it.next();
						int center;
						if (nucleosomeSize == null || nucleosomeSize <= 0) {
							center = entry.center();
						} else {
							if (entry.isWatson()) {
								center = entry.getStart() + nucleosomeSize/2;
							} else {
								center = entry.getStart() - nucleosomeSize/2;
							}
						}
						
						// Only map if it is in the current chunk
						if (chunkStart <= center && center <= chunkStop) {
							count[center-chunkStart]++;
							mapped++;
						}
					}
					
					// Write the count at each base pair to the output file
					writer.write(new Contig(chunk, count));
					
					// Process the next chunk
					chunkStart = chunkStop + 1;
				}
			}
		}
		
		log.info("Mapped "+mapped+" reads");
	}
	
	public static void main(String[] args) {
		new MapDyads().instanceMain(args);
	}
}