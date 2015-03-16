package edu.unc.genomics.wigmath;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.ngs.Autocorrelation;
import edu.unc.utils.FloatHistogram;

/**
 * Make a histogram of the values in a Wig file
 * 
 * @author timpalpant
 *
 */
public class ValueDistribution extends CommandLineTool {

  private static final Logger log = Logger.getLogger(Autocorrelation.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-l", "--min" }, description = "Minimum bin value")
  public Float min;
  @Parameter(names = { "-h", "--max" }, description = "Maximum bin value")
  public Float max;
  @Parameter(names = { "-n", "--bins" }, description = "Number of bins")
  public int numBins = 40;
  @Parameter(names = { "-o", "--output" }, description = "Output file")
  public Path outputFile;

  FloatHistogram hist;
  double mean, stdev, skewness, kurtosis;
  long N;

  public void run() throws IOException {
    try (WigFileReader reader = WigFileReader.autodetect(inputFile)) {
      log.debug("Generating histogram of Wig values");
      if (min == null) {
        min = (float) reader.min();
      }
      if (max == null) {
        max = (float) reader.max();
      }
      hist = new FloatHistogram(numBins, min, max);

      N = reader.numBases();
      mean = reader.mean();
      stdev = reader.stdev();

      // Also compute the skewness and kurtosis while going through the values
      double sumOfCubeOfDeviances = 0;
      double sumOfFourthOfDeviances = 0;

      for (String chr : reader.chromosomes()) {
        int start = reader.getChrStart(chr);
        int stop = reader.getChrStop(chr);
        log.debug("Processing chromosome " + chr + " region " + start + "-" + stop);

        // Process the chromosome in chunks
        int bp = start;
        while (bp < stop) {
          int chunkStart = bp;
          int chunkStop = Math.min(bp + DEFAULT_CHUNK_SIZE - 1, stop);
          Interval chunk = new Interval(chr, chunkStart, chunkStop);
          log.debug("Processing chunk " + chunk);

          try {
            float[] data = reader.query(chunk).getValues();
            for (int i = 0; i < data.length; i++) {
              if (!Float.isNaN(data[i]) && !Float.isInfinite(data[i])) {
                hist.addValue(data[i]);
                double deviance = data[i] - mean;
                double cubeOfDeviance = Math.pow(deviance, 3);
                sumOfCubeOfDeviances += cubeOfDeviance;
                sumOfFourthOfDeviances += deviance * cubeOfDeviance;
              }
            }
          } catch (WigFileException e) {
            log.error("Error getting data from Wig file for chunk " + chunk);
            throw new CommandLineToolException("Error getting data from Wig file for chunk " + chunk);
          }

          bp = chunkStop + 1;
        }
      }

      skewness = (sumOfCubeOfDeviances / N) / Math.pow(stdev, 3);
      kurtosis = (sumOfFourthOfDeviances / N) / Math.pow(stdev, 4);
    }

    // Construct the output summary
    StringBuilder sb = new StringBuilder();
    sb.append("Moments:\tmean(w) = " + mean + "\n");
    sb.append("\t\tvar(w) = " + stdev * stdev + "\n");
    sb.append("\t\tskew(w) = " + skewness + "\n");
    sb.append("\t\tkur(w) = " + kurtosis + "\n");
    sb.append("Histogram:" + "\n");
    sb.append(hist);

    if (outputFile != null) {
      log.debug("Writing to output file");
      try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
        writer.write(sb.toString());
      }
    } else {
      System.out.println(sb.toString());
    }
  }

  public static void main(String[] args) throws IOException, WigFileException {
    new ValueDistribution().instanceMain(args);
  }

}
