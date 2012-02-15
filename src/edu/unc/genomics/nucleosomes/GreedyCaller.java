package edu.unc.genomics.nucleosomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.SortUtils;

public class GreedyCaller extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(GreedyCaller.class);
	
	private static final int CHUNK_SIZE = 500_000;

	@Parameter(names = {"-d", "--dyads"}, description = "Dyad counts file", required = true, validateWith = ReadablePathValidator.class)
	public WigFile dyadsFile;
	@Parameter(names = {"-s", "--smoothed"}, description = "Smoothed dyad counts file", required = true, validateWith = ReadablePathValidator.class)
	public WigFile smoothedDyadsFile;
	@Parameter(names = {"-n", "--size"}, description = "Nucleosome size (bp)")
	public int nucleosomeSize = 147;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	
	public void run() throws IOException {
		int halfNuc = nucleosomeSize / 2;
		int count = 0;
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			for (String chr : smoothedDyadsFile.chromosomes()) {
				log.debug("Processing chromosome "+chr);
				int chunkStart = smoothedDyadsFile.getChrStart(chr);
				int chrStop = smoothedDyadsFile.getChrStop(chr);
				while (chunkStart < chrStop) {
					int chunkStop = Math.min(chunkStart+CHUNK_SIZE-1, smoothedDyadsFile.getChrStop(chr));
					int paddedStart = Math.max(chunkStart-nucleosomeSize, 1);
					int paddedStop = Math.min(chunkStop+nucleosomeSize, smoothedDyadsFile.getChrStop(chr));
					log.debug("Processing chunk "+chunkStart+"-"+chunkStop);
					
					log.debug("Loading data and sorting");
					Iterator<WigItem> dyadsIter;
					Iterator<WigItem> smoothedIter;
					try {
						dyadsIter = dyadsFile.query(chr, paddedStart, paddedStop);
						smoothedIter = smoothedDyadsFile.query(chr, paddedStart, paddedStop);
					} catch (IOException | WigFileException e) {
						e.printStackTrace();
						throw new CommandLineToolException("Error loading data from Wig file");
					}
					
					float[] dyads = WigFile.flattenData(dyadsIter, paddedStart, paddedStop);
					float[] smoothed = WigFile.flattenData(smoothedIter, paddedStart, paddedStop);
					int[] sortedIndices = SortUtils.indexSort(smoothed);

					// Proceed through the data in descending order
					log.debug("Calling nucleosomes");
					for (int j = sortedIndices.length-1; j >= 0; j--) {
						int i = sortedIndices[j];
						int dyad = paddedStart + i;
						
						if (smoothed[i] > 0) {
							int nucStart = Math.max(paddedStart, dyad-halfNuc);
							int nucStop = Math.min(dyad+halfNuc, paddedStop);
							NucleosomeCall call = new NucleosomeCall(chr, nucStart, nucStop);
							call.setDyad(dyad);
							
							double occupancy = 0;
							double weightedSum = 0;
							double smoothedSum = 0;
							double sumOfSquares = 0;
							for (int bp = nucStart; bp <= nucStop; bp++) {
								occupancy += dyads[bp-paddedStart];
								weightedSum += bp * dyads[bp-paddedStart];
								smoothedSum += smoothed[bp-paddedStart];
								sumOfSquares += bp * bp * dyads[bp-paddedStart];
							}
							call.setOccupancy(occupancy);
							
							if (occupancy > 0) {
								call.setDyadMean((int)Math.round(weightedSum/occupancy));
								call.setConditionalPosition(smoothed[i] / smoothedSum);
								double variance = (sumOfSquares - weightedSum*call.getDyadMean()) / occupancy;
								call.setDyadStdev(Math.sqrt(variance));
								
								// Only write nucleosomes within the current chunk to disk
								if (chunkStart <= dyad && dyad <= chunkStop) {
									writer.write(call.toString());
									writer.newLine();
									count++;
								}
								
								// Don't allow nucleosome calls overlapping this nucleosome
								int low = Math.max(i-nucleosomeSize, 0);
								int high = Math.min(i+nucleosomeSize, smoothed.length-1);
								for (int k = low; k <= high; k++) {
									smoothed[k] = 0;
								}
							}
						}
					}
					
					chunkStart = chunkStop + 1;
				}
			}
		}
		
		log.info("Called "+count+" nucleosomes");
	}
	
	public static void main(String[] args) {
		new GreedyCaller().instanceMain(args);
	}
}