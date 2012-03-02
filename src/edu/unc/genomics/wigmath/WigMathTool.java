package edu.unc.genomics.wigmath;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

/**
 * Abstract class for writing programs to do computation on Wig files
 * Concrete subclasses must implement the compute method
 * 
 * @author timpalpant
 *
 */
public abstract class WigMathTool extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(WigMathTool.class);
	
	// TODO: Variable resolution output?
	
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	
	protected List<WigFile> inputs = new ArrayList<WigFile>();
	
	public void addInputFile(WigFile wig) {
		inputs.add(wig);
	}
	
	/**
	 * Setup the computation. Should add all input Wig files
	 * with addInputFile() during setup
	 */
	public abstract void setup();
	
	/**
	 * Do the computation on a chunk and return the results
	 * Must return (stop-start+1) values
	 * 
	 * @param chr
	 * @param start
	 * @param stop
	 * @return the results of the computation for this chunk
	 * @throws IOException
	 * @throws WigFileException
	 */
	public abstract float[] compute(String chr, int start, int stop)
			 throws IOException, WigFileException;
	
	@Override
	public void run() throws IOException {
		log.debug("Executing setup operations");
		setup();
		
		log.debug("Processing files and writing result to disk");
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wig header
			writer.write("track type=wiggle_0");
			writer.newLine();
			
			Set<String> chromosomes = getCommonChromosomes(inputs);
			log.debug("Found " + chromosomes.size() + " chromosomes in common between all inputs");
			for (String chr : chromosomes) {
				int start = getMaxChrStart(inputs, chr);
				int stop = getMinChrStop(inputs, chr);
				log.debug("Processing chromosome " + chr + " region " + start + "-" + stop);
				
				// Write the chromosome header to output
				writer.write("fixedStep chrom="+chr+" start="+start+" step=1 span=1");
				writer.newLine();
				
				// Process the chromosome in chunks
				int bp = start;
				while (bp < stop) {
					int chunkStart = bp;
					int chunkStop = Math.min(bp+DEFAULT_CHUNK_SIZE-1, stop);
					int expectedLength = chunkStop - chunkStart + 1;
					log.debug("Processing chunk "+chr+":"+chunkStart+"-"+chunkStop);
					
					float[] result = null;
					try {
						result = compute(chr, chunkStart, chunkStop);
					} catch (WigFileException e) {
						log.fatal("Wig file error while processing chunk " + chr + " region " + start + "-" + stop);
						e.printStackTrace();
						throw new RuntimeException("Wig file error while processing chunk " + chr + " region " + start + "-" + stop);
					}
					
					if (result.length != expectedLength) {
						log.error("Expected result length="+expectedLength+", got="+result.length);
						throw new RuntimeException("Result is not the expected length!");
					}
	
					for (int i = 0; i < result.length; i++) {
						writer.write(Float.toString(result[i]));
						writer.newLine();
					}
					
					bp = chunkStop + 1;
				}
			}
		}
		
		for (WigFile wig : inputs) {
			wig.close();
		}
	}
	
	public int getMaxChrStart(List<WigFile> wigs, String chr) {
		int max = -1;
		for (WigFile wig : wigs) {
			if (wig.getChrStart(chr) > max) {
				max = wig.getChrStart(chr);
			}
		}
		
		return max;
	}
	
	public int getMinChrStop(List<WigFile> wigs, String chr) {
		if (wigs.size() == 0) {
			return -1;
		}
		
		int min = Integer.MAX_VALUE;
		for (WigFile wig : wigs) {
			if (wig.getChrStop(chr) < min) {
				min = wig.getChrStop(chr);
			}
		}
		
		return min;
	}
	
	public Set<String> getCommonChromosomes(List<WigFile> wigs) {
		if (wigs.size() == 0) {
			return new HashSet<String>();
		}
		
		Set<String> chromosomes = wigs.get(0).chromosomes();
		Iterator<String> it = chromosomes.iterator();
		while(it.hasNext()) {
			String chr = it.next();
			for (WigFile wig : wigs) {
				if (!wig.includes(chr)) {
					it.remove();
					break;
				}
			}
		}

		return chromosomes;
	}
}
