package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.IntervalFileWriter;

/**
 * Randomly select N reads out of a total of M
 * 
 * @author timpalpant
 *
 */
public class Subsample extends CommandLineTool {

  private static final Logger log = Logger.getLogger(Subsample.class);

  @Parameter(names = { "-i", "--input" }, required = true, description = "Input file", validateWith = ReadablePathValidator.class)
  public Path input;
  @Parameter(names = { "-n", "--select" }, required = true, description = "Number of entries to select")
  public int n;
  @Parameter(names = { "-o", "--output" }, required = true, description = "Output file")
  public Path output;

  @Override
  public void run() throws IOException {
    try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(input);
        IntervalFileWriter<Interval> writer = new IntervalFileWriter<>(output)) {
      int nRemaining = reader.count();
      log.info("Input file has " + nRemaining + " entries");
      if (n >= reader.count()) {
        throw new CommandLineToolException("Cannot select " + n + " entries from a file with " + nRemaining);
      }

      // See http://eyalsch.wordpress.com/2010/04/01/random-sample/
      // for a nice summary of different algorithms to randomly pick n entries
      log.info("Randomly selecting " + n + " entries");
      Random rng = new Random();
      for (Interval entry : reader) {
        if (n == 0) {
          break;
        } else if (rng.nextDouble() < ((double) n) / nRemaining) {
          writer.write(entry);
          n--;
        }

        nRemaining--;
      }
    }
  }

  public static void main(String[] args) {
    new Subsample().instanceMain(args);
  }
}