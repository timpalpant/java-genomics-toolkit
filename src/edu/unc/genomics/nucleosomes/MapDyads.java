package edu.unc.genomics.nucleosomes;

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
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;

public class MapDyads extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(MapDyads.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads)", required = true)
	public IntervalFile<? extends Interval> inputFile;
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
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wiggle track header to the output file
			TrackHeader header = new TrackHeader("wiggle_0");
			header.setName("Converted " + inputFile.getPath().getFileName());
			header.setDescription("Converted " + inputFile.getPath().getFileName());
			writer.write(header.toString());
			writer.newLine();
			
			// Process each chromosome in the assembly
			for (String chr : inputFile.chromosomes()) {
				if (!assembly.includes(chr)) {
					continue;
				}
				
				log.debug("Processing chromosome " + chr);
				// Write the contig header to the output file
				writer.write("fixedStep chrom="+chr+" start=1 step=1 span=1");
				writer.newLine();
				
				int start = 1;
				while (start < assembly.getChrLength(chr)) {
					int stop = Math.min(start+DEFAULT_CHUNK_SIZE-1, assembly.getChrLength(chr));
					int length = stop - start + 1;
					int[] count = new int[length];
					
					Iterator<? extends Interval> it = inputFile.query(chr, start, stop);
					while (it.hasNext()) {
						Interval entry = it.next();
						int center;
						if (nucleosomeSize == null || nucleosomeSize == -1) {
							center = entry.center();
						} else {
							center = entry.getStart() + nucleosomeSize/2;
						}
						
						// Only map if it is in the current chunk
						if (start <= center && center <= stop) {
							count[center-start]++;
							mapped++;
						}
					}
					
					// Write the average at each base pair to the output file
					for (int i = 0; i < count.length; i++) {
						writer.write(Integer.toString(count[i]));
						writer.newLine();
					}
					
					// Process the next chunk
					start = stop + 1;
				}
			}
		}
		
		log.info("Mapped "+mapped+" reads");
	}
	
	public static void main(String[] args) {
		new MapDyads().instanceMain(args);
	}
}