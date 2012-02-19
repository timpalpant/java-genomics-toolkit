package edu.unc.genomics.nucleosomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class Phasogram extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(Phasogram.class);

	@Parameter(names = {"-i", "--input"}, description = "Input wig file (read counts)", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-m", "--max"}, description = "Maximum phase shift", required = true)
	public int maxPhase;
	@Parameter(names = {"-o", "--output"}, description = "Output file (histogram)", required = true)
	public Path outputFile;
		
	public void run() throws IOException {
		long[] phaseCounts = new long[maxPhase+1];
		
		// Process each chromosome in the assembly
		for (String chr : inputFile.chromosomes()) {
			log.debug("Processing chromosome " + chr);
						
			int start = inputFile.getChrStart(chr);
			while (start < inputFile.getChrStop(chr)) {
				int stop = Math.min(start+DEFAULT_CHUNK_SIZE-1, inputFile.getChrStop(chr));
								
				try {
					float[] data = WigFile.flattenData(inputFile.query(chr, start, stop), start, stop);
					for (int i = 0; i < data.length-maxPhase; i++) {
						for (int j = 0; j <= maxPhase; j++) {
							phaseCounts[j] += data[i];
						}
					}
					
				} catch (WigFileException e) {
					log.fatal("Error querying data from Wig file!");
					e.printStackTrace();
					throw new CommandLineToolException("Error querying data from Wig file!");
				}
				
				// Process the next chunk
				start = stop - maxPhase + 1;
			}
		}
		
		log.debug("Writing output to disk");
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			for (int i = 0; i < phaseCounts.length; i++) {
				writer.write(i+"\t"+phaseCounts[i]);
				writer.newLine();
			}
		}
	}
	
	public static void main(String[] args) {
		new Phasogram().instanceMain(args);
	}
}