package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.io.WigFileWriter;

/**
 * Abstract base class for writing programs to do computation on Wig files
 * Concrete subclasses must implement the compute method
 * 
 * WigMathTool takes all input Wig files, finds the intersecting set of
 * chromosomes with data, and then iterates through the inputs in a
 * chunk-by-chunk fashion, calling compute() on each chunk
 * 
 * The compute method must return the output values for that chunk (one value
 * for each base pair) which will then be written into a new output Wig file.
 * 
 * @author timpalpant
 * 
 */
public abstract class WigMathTool extends CommandLineTool {

	private static final Logger log = Logger.getLogger(WigMathTool.class);

	@Parameter(names = { "-c", "--chunk" }, description = "Size to chunk each chromosome into when processing (bp)")
	public int chunkSize = DEFAULT_CHUNK_SIZE;
	@Parameter(names = { "-p", "--threads" }, description = "Number of threads to use")
	public int nThreads = 1;
	@Parameter(names = { "-f", "--fixedstep" }, description = "Force fixedStep output")
	public boolean fixedStep = false;
	@Parameter(names = { "-o", "--output" }, description = "Output file", required = true)
	public Path outputFile;

	private ExecutorService pool;

	/**
	 * Holds all of the input Wig files for this compute job. Used to find the
	 * intersecting set of chromosomes to process
	 */
	protected List<WigFileReader> inputs = new ArrayList<WigFileReader>();

	protected void addInputFile(WigFileReader wig) {
		inputs.add(wig);
	}

	/**
	 * Setup the computation. Should add all input Wig files with addInputFile()
	 * during setup
	 */
	protected abstract void setup();

	/**
	 * Do the computation on a chunk and return the results Must return
	 * chunk.length() values (one for every base pair in chunk)
	 * 
	 * @param chunk
	 *            the interval to process
	 * @return the results of the computation for this chunk
	 * @throws IOException
	 * @throws WigFileException
	 */
	protected abstract float[] compute(Interval chunk) throws IOException, WigFileException;

	@Override
	public final void run() throws IOException {
		log.debug("Executing setup operations");
		setup();
		
		log.debug("Initializing thread pool");
		pool = Executors.newFixedThreadPool(nThreads);
		
		log.debug("Processing files and writing result to disk");
		List<Future<?>> futures = new ArrayList<>();
		try (WigFileWriter writer = new WigFileWriter(outputFile, TrackHeader.newWiggle())) {
			Set<String> chromosomes = getCommonChromosomes(inputs);
			log.debug("Found " + chromosomes.size() + " chromosomes in common between all inputs");
			for (String chr : chromosomes) {
				int start = getMaxChrStart(inputs, chr);
				int stop = getMinChrStop(inputs, chr);
				
				// Process the chromosome in chunks
				int bp = start;
				while (bp < stop) {
					int chunkStart = bp;
					int chunkStop = Math.min(bp+chunkSize-1, stop);
					final Interval chunk = new Interval(chr, chunkStart, chunkStop);
					
					Future<?> future = pool.submit(new Runnable() {

                      @Override
                       public void run() {
                        	log.debug("Processing chunk "+chunk);
                        	float[] result;
        					try {
        						result = compute(chunk);
        					} catch (Exception e) {
        						throw new CommandLineToolException("Exception while processing chunk "+chunk, e);
        					}
        					
        					// Verify that the computation returned the correct number of values for the chunk
        					if (result.length != chunk.length()) {
        						log.error("Expected result length="+chunk.length()+", got="+result.length);
        						throw new CommandLineToolException("Result of Wig computation is not the expected length!");
        					}

        					// Write the result of the computation for this chunk to disk
        					if (fixedStep) {
        						writer.writeFixedStepContig(new Contig(chunk, result));
        					} else {
        						writer.write(new Contig(chunk, result));
        					}
                        }
					  
					});
					
					futures.add(future);
					
					// Move to the next chunk
					bp = chunkStop + 1;
				}
			}
			
			for (Future<?> f : futures) {
				f.get();
			}
		} catch (InterruptedException | ExecutionException e) {
			pool.shutdownNow();
			throw new CommandLineToolException(e);
		} finally {
			close();
		}
	}

	/**
	 * Close the input files
	 * 
	 * @throws IOException
	 */
	private void close() throws IOException {
		for (WigFileReader wig : inputs) {
			wig.close();
		}
	}

	/**
	 * Gets the highest start base for a chromosome amongst all wigs
	 * 
	 * @param wigs
	 *            a List of wig files
	 * @param chr
	 *            the chromosome to get the most conservative start base for
	 * @return the highest start base amongst all of the Wig files in wigs
	 */
	public static int getMaxChrStart(List<WigFileReader> wigs, String chr) {
		int max = -1;
		for (WigFileReader wig : wigs) {
			if (wig.getChrStart(chr) > max) {
				max = wig.getChrStart(chr);
			}
		}

		return max;
	}

	/**
	 * Gets the lowest stop base for a chromosome amongst all wigs
	 * 
	 * @param wigs
	 *            a List of wig files
	 * @param chr
	 *            the chromosome to get the most conservative stop base for
	 * @return the lowest stop base amongst all of the Wig files in wigs
	 */
	public static int getMinChrStop(List<WigFileReader> wigs, String chr) {
		if (wigs.size() == 0) {
			return -1;
		}

		int min = Integer.MAX_VALUE;
		for (WigFileReader wig : wigs) {
			if (wig.getChrStop(chr) < min) {
				min = wig.getChrStop(chr);
			}
		}

		return min;
	}

	/**
	 * Get the set of chromosomes that are held in common by all input files
	 * 
	 * @param wigs
	 *            a list of Wig files to get the common chromosomes of
	 * @return the set of chromosomes held in common by all Wig files in wigs
	 */
	public static Set<String> getCommonChromosomes(List<WigFileReader> wigs) {
		if (wigs == null || wigs.size() == 0) {
			return new HashSet<String>();
		}

		Set<String> chromosomes = wigs.get(0).chromosomes();
		Iterator<String> it = chromosomes.iterator();
		while (it.hasNext()) {
			String chr = it.next();
			for (WigFileReader wig : wigs) {
				if (!wig.includes(chr)) {
					it.remove();
					break;
				}
			}
		}

		return chromosomes;
	}
}
