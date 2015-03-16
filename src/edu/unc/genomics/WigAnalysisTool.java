package edu.unc.genomics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.Interval;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Abstract base class for writing programs to do analysis on Wig files
 * 
 * WigAnalysisTool takes all input Wig files, finds the intersecting set of
 * chromosomes with data, and then iterates through the inputs in a
 * chunk-by-chunk fashion, calling compute() on each chunk
 * 
 * prepare() is called before the computation loop shutdown() is called after
 * the computation loop
 * 
 * @author timpalpant
 * 
 */
public abstract class WigAnalysisTool extends CommandLineTool {

  private static final Logger log = Logger.getLogger(WigAnalysisTool.class);

  @Parameter(names = { "-c", "--chunk" }, description = "Maximum number of data values to load per thread (bp)")
  public int chunkSize = DEFAULT_CHUNK_SIZE;
  @Parameter(names = { "-p", "--threads" }, description = "Number of threads to use")
  public int nThreads = 1;

  private ExecutorService pool;
  /**
   * Process the union of the extents of all input files rather than the
   * intersection
   */
  protected boolean unionExtents = false;

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
  protected abstract void prepare();

  /**
   * Optional shutdown the computation, do any cleanup, final processing
   * 
   * @throws IOException
   */
  protected void shutdown() throws IOException {
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
   * Do the computation on a chunk and return the results Must return
   * chunk.length() values (one for every base pair in chunk)
   * 
   * @param chunk
   *          the interval to process
   * @return the results of the computation for this chunk
   * @throws IOException
   * @throws WigFileException
   */
  protected abstract void process(Interval chunk) throws IOException, WigFileException;

  @Override
  public final void run() throws IOException {
    log.debug("Executing setup operations");
    prepare();

    Set<String> chromosomes = null;
    if (unionExtents) {
      chromosomes = getUnionChromosomes(inputs);
      log.debug("Found " + chromosomes.size() + " chromosomes in the union of all inputs");
    } else {
      chromosomes = getIntersectionChromosomes(inputs);
      log.debug("Found " + chromosomes.size() + " chromosomes in the intersection of all inputs");
    }

    log.debug("Initializing thread pool with " + nThreads + " threads");
    pool = Executors.newFixedThreadPool(nThreads);

    log.debug("Performing main computation");
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (String chr : chromosomes) {
        Interval interval = unionExtents ? getUnion(inputs, chr) : getIntersection(inputs, chr);

        // Process the chromosome in chunks
        int bp = interval.low();
        while (bp < interval.high()) {
          int chunkStart = bp;
          int chunkStop = Math.min(bp + chunkSize - 1, interval.high());
          final Interval chunk = new Interval(chr, chunkStart, chunkStop);

          futures.add(pool.submit(new Runnable() {

            @Override
            public void run() {
              log.debug("Processing chunk " + chunk);
              try {
                process(chunk);
              } catch (Exception e) {
                throw new CommandLineToolException("Exception while processing chunk " + chunk, e);
              }
            }

          }));

          // Move to the next chunk
          bp = chunkStop + 1;
        }
      }

      for (Future<?> f : futures) {
        f.get();
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new CommandLineToolException(e);
    } catch (IntervalException e) {
      throw new CommandLineToolException(e);
    } finally {
      pool.shutdownNow();
      shutdown();
      close();
    }
  }

  /**
   * Get the set of chromosomes that are held in common by all input files
   * 
   * @param wigs
   *          a list of Wig files to get the common chromosomes of
   * @return the set of chromosomes held in common by all Wig files in wigs
   */
  public static Set<String> getIntersectionChromosomes(List<WigFileReader> wigs) {
    if (wigs == null || wigs.isEmpty()) {
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

  /**
   * Gets the intersecting Interval for a chromosome amongst all wigs
   * 
   * @param wigs
   *          a List of wig files
   * @param chr
   *          the chromosome to get the most conservative start base for
   * @return an Interval for which all wigs have data
   */
  public static Interval getIntersection(List<WigFileReader> wigs, String chr) {
    if (wigs == null || wigs.isEmpty()) {
      return null;
    }

    Interval intersection = wigs.get(0).getChrExtents(chr);
    for (int i = 1; i < wigs.size(); i++) {
      if (intersection == null)
        break;
      intersection = intersection.intersection(wigs.get(i).getChrExtents(chr));
    }

    return intersection;
  }

  /**
   * Get the set of chromosomes that are held in common by all input files
   * 
   * @param wigs
   *          a list of Wig files to get the common chromosomes of
   * @return the set of chromosomes held in common by all Wig files in wigs
   */
  public static Set<String> getUnionChromosomes(List<WigFileReader> wigs) {
    Set<String> chromosomes = new HashSet<>();
    for (WigFileReader wig : wigs) {
      chromosomes.addAll(wig.chromosomes());
    }

    return chromosomes;
  }

  /**
   * Gets the union Interval for a chromosome amongst all wigs
   * 
   * @param wigs
   *          a List of wig files
   * @param chr
   *          the chromosome to get the most conservative start base for
   * @return an Interval for which at least one wig has data
   * @throws IntervalException
   *           if one of the wigs does not contain chr
   */
  public static Interval getUnion(List<WigFileReader> wigs, String chr) throws IntervalException {
    if (wigs == null || wigs.isEmpty()) {
      return null;
    }

    Interval union = null;
    for (WigFileReader wig : wigs) {
      if (!wig.includes(chr))
        continue;
      union = wig.getChrExtents(chr).union(union);
    }

    return union;
  }
}
