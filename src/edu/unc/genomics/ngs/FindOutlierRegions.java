package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.BedGraphFileWriter;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Finds regions of a Wig file that differ significantly from the mean, such as
 * CNVs or deletions.
 * 
 * @author timpalpant
 *
 */
public class FindOutlierRegions extends CommandLineTool {

  private static final Logger log = Logger.getLogger(FindOutlierRegions.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-w", "--window" }, description = "Window size", required = true)
  public int windowSize;
  @Parameter(names = { "-t", "--threshold" }, description = "Threshold (fold x mean)")
  public float fold = 3;
  @Parameter(names = { "-b", "--below" }, description = "Search for outliers below the threshold")
  public boolean below = false;
  @Parameter(names = { "-o", "--output" }, description = "Output file (bedGraph)", required = true)
  public Path outputFile;

  int flip = 1;
  double threshold;
  DescriptiveStatistics stats = new DescriptiveStatistics();

  @Override
  public void run() throws IOException {
    log.debug("Scanning with " + windowSize + " bp window");
    stats.setWindowSize(windowSize);
    if (below) {
      flip = -1;
    }

    // Run through the genome finding regions that exceed the threshold
    try (WigFileReader reader = WigFileReader.autodetect(inputFile);
        BedGraphFileWriter<Interval> writer = new BedGraphFileWriter<Interval>(outputFile)) {
      threshold = fold * reader.mean();
      log.debug("Threshold = " + threshold);

      for (String chr : reader.chromosomes()) {
        int start = reader.getChrStart(chr);
        int stop = reader.getChrStop(chr);
        log.debug("Processing chromosome " + chr + ":" + start + "-" + stop);

        // Process the chromosome in chunks
        int bp = start;
        Interval outlier = null;
        stats.clear();
        while (bp < stop) {
          int chunkStart = bp;
          int chunkStop = Math.min(bp + DEFAULT_CHUNK_SIZE - 1, stop);
          Interval chunk = new Interval(chr, chunkStart, chunkStop);
          log.debug("Processing chunk " + chunk);

          try {
            float[] data = reader.query(chunk).getValues();
            for (int i = 0; i < data.length; i++) {
              stats.addValue(data[i]);

              // If the mean of the current window is > threshold
              // write it to output as a potential outlier region
              if (outlier == null) {
                // Start a new outlier region
                if (flip * stats.getMean() > flip * threshold) {
                  outlier = new Interval(chr, chunkStart + i - windowSize, -1);
                }
              } else {
                // End an outlier region
                if (flip * stats.getMean() < flip * threshold) {
                  outlier.setStop(chunkStart + i);
                  writer.write(outlier);
                  outlier = null;
                }
              }
            }

          } catch (WigFileException e) {
            log.fatal("Wig file error while processing chunk " + chr + ":" + start + "-" + stop);
            e.printStackTrace();
            throw new RuntimeException("Wig file error while processing chunk " + chr + ":" + start + "-" + stop);
          }

          bp = chunkStop + 1;
        }

        // If there is an open outlier region at the end of the chromosome
        if (outlier != null) {
          outlier.setStop(stop);
          writer.write(outlier);
          outlier = null;
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
    new FindOutlierRegions().instanceMain(args);
  }

}
