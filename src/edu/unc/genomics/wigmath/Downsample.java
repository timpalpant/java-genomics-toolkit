package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.WigStatistic;

/**
 * Downsample a high-resolution Wig file into larger windows so that it has
 * smaller file size
 * 
 * @author timpalpant
 *
 */
public class Downsample extends CommandLineTool {

  private static final Logger log = Logger.getLogger(Downsample.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-w", "--window" }, description = "Window size (bp)")
  public int windowSize = 100;
  @Parameter(names = { "-m", "--metric" }, description = "Downsampling metric (coverage/total/mean/min/max)")
  public String metric = "mean";
  @Parameter(names = { "-o", "--output" }, description = "Output file", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    WigStatistic dsm = WigStatistic.fromName(metric);
    if (dsm == null) {
      log.error("Unknown downsampling metric: " + metric);
      throw new CommandLineToolException("Unknown downsampling metric: " + metric
          + ". Options are mean, min, max, coverage, total");
    } else {
      log.debug("Using downsampling metric: " + metric);
    }

    try (WigFileReader reader = WigFileReader.autodetect(inputFile);
        PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, Charset.defaultCharset()))) {
      // Write the Wig header
      TrackHeader header = TrackHeader.newWiggle();
      header.setName("Downsampled " + inputFile.getFileName());
      header.setDescription("Downsampled " + inputFile.getFileName());
      writer.println(header);

      for (String chr : reader.chromosomes()) {
        log.debug("Processing chromosome " + chr);
        int start = reader.getChrStart(chr);
        int stop = reader.getChrStop(chr);

        // Write the chromosome header to output
        writer.println("fixedStep chrom=" + chr + " start=" + start + " step=" + windowSize + " span=" + windowSize);

        // Process the chromosome in chunks
        int bp = start;
        while (bp < stop) {
          int chunkStart = bp;
          int chunkStop = Math.min(bp + windowSize - 1, stop);

          try {
            // Get the original data for this window from the Wig file
            SummaryStatistics result = reader.queryStats(chr, chunkStart, chunkStop);
            // Do the downsampling
            float value = Float.NaN;
            switch (dsm) {
            case COVERAGE:
              value = result.getN();
              break;
            case TOTAL:
              value = (float) result.getSum();
              break;
            case MEAN:
              value = (float) result.getMean();
              break;
            case MIN:
              value = (float) result.getMin();
              break;
            case MAX:
              value = (float) result.getMax();
              break;
            }
            // Write the downsampled value to the output file
            writer.println(value);
          } catch (WigFileException e) {
            log.error("Error querying Wig file for data from interval " + chr + ":" + chunkStart + "-" + chunkStop);
            e.printStackTrace();
            throw new CommandLineToolException("Error querying Wig file for data from interval " + chr + ":"
                + chunkStart + "-" + chunkStop);
          }

          bp = chunkStop + 1;
        }
      }
    }
  }

  public static void main(String[] args) {
    new Downsample().instanceMain(args);
  }

}
