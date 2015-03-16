package edu.unc.genomics.ngs;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.math3.stat.Frequency;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * Generate a histogram of interval lengths, such as read lengths or gene
 * lengths
 * 
 * @author timpalpant
 *
 */
public class IntervalLengthDistribution extends CommandLineTool {

  private static final Logger log = Logger.getLogger(IntervalLengthDistribution.class);

  @Parameter(names = { "-i", "--input" }, description = "Interval file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-f", "--freq" }, description = "Output frequencies rather than counts")
  public boolean outputFreq = false;
  @Parameter(names = { "-o", "--output" }, description = "Output file", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    log.debug("Generating histogram of interval lengths");
    Frequency freq = new Frequency();
    int min = Integer.MAX_VALUE;
    int max = -1;
    try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(inputFile)) {
      for (Interval i : reader) {
        int L = i.length();
        freq.addValue(L);

        if (L < min) {
          min = L;
        }
        if (L > max) {
          max = L;
        }
      }
    }

    log.debug("Writing histogram output");
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, Charset.defaultCharset()))) {
      for (int i = min; i <= max; i++) {
        if (outputFreq) {
          writer.println(i + "\t" + freq.getPct(i));
        } else {
          writer.println(i + "\t" + freq.getCount(i));
        }
      }
    }
  }

  public static void main(String[] args) {
    new IntervalLengthDistribution().instanceMain(args);
  }

}
