package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.io.WigFileWriter;

/**
 * Removes regions of a Wig file
 * 
 * @author timpalpant
 *
 */
public class FilterRegions extends CommandLineTool {

  private static final Logger log = Logger.getLogger(FilterRegions.class);

  @Parameter(names = { "-i", "--input" }, description = "Input (Big)wig", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-l", "--loci" }, description = "Loci file (Bed)", required = true, validateWith = ReadablePathValidator.class)
  public Path lociFile;
  @Parameter(names = { "-o", "--output" }, description = "Output file (wig)", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    // Run through the genome, copying data from the input wig to the output wig
    // and setting filtered regions to NaN
    try (WigFileReader reader = WigFileReader.autodetect(inputFile);
        IntervalFileReader<? extends Interval> loci = IntervalFileReader.autodetect(lociFile);
        WigFileWriter writer = new WigFileWriter(outputFile, reader.getHeader())) {
      for (String chr : reader.chromosomes()) {
        int bp = reader.getChrStart(chr);
        int stop = reader.getChrStop(chr);
        log.debug("Processing chromosome " + chr + " region " + bp + "-" + stop);

        // Process the chromosome in chunks
        while (bp < stop) {
          int chunkStart = bp;
          int chunkStop = Math.min(bp + DEFAULT_CHUNK_SIZE - 1, stop);
          Interval chunk = new Interval(chr, chunkStart, chunkStop);
          log.debug("Processing chunk " + chunk);

          try {
            Contig contig = reader.query(chunk);
            Iterator<? extends Interval> it = loci.query(contig);
            int start = contig.getStart();
            // Copy the data up to the excluded point, then skip past the
            // excluded point
            while (it.hasNext()) {
              Interval exclude = it.next();
              log.debug("Skipping interval " + exclude);
              if (exclude.low() - 1 > start) {
                writer.write(contig.copy(start, exclude.low() - 1));
              }
              start = exclude.high() + 1;
            }

            // Copy the remaining data, if there is any
            if (start <= contig.getStop()) {
              writer.write(contig.copy(start, contig.getStop()));
            }
          } catch (WigFileException e) {
            throw new CommandLineToolException("Wig file error while processing chunk " + chr + ":" + chunkStart + "-"
                + chunkStop, e);
          }

          bp = chunkStop + 1;
        }
      }
    }
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new FilterRegions().instanceMain(args);
  }

}
