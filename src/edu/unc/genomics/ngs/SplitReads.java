package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.IntervalFileWriter;

/**
 * This tool splits sequencing reads into bins
 * 
 * @author timpalpant
 *
 */
public class SplitReads extends CommandLineTool {

  private static final Logger log = Logger.getLogger(SplitReads.class);

  @Parameter(names = { "-i", "--input" }, required = true, description = "Input file", validateWith = ReadablePathValidator.class)
  public Path input;
  @Parameter(names = { "-b", "--bins" }, description = "Number of bins to split reads into")
  public int bins = 5;
  @Parameter(names = { "-o", "--output" }, description = "Output file")
  public Path output;

  @Override
  public void run() throws IOException {
    // Prepare the outputs
    String[] splitName = output.toString().split("\\.(?=[^\\.]+$)");
    String basename = splitName[0];
    String ext = splitName[1];
    List<IntervalFileWriter<Interval>> writers = new ArrayList<>();
    try {
      for (int i = 0; i < bins; i++) {
        Path outFile = output.resolve(basename + '.' + i + '.' + ext);
        IntervalFileWriter<Interval> writer = new IntervalFileWriter<>(outFile);
        writers.add(writer);
      }

      try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(input)) {
        int current = 0;
        int count = 0;
        for (Interval interval : reader) {
          writers.get(current).write(interval);
          current++;
          current %= bins;
          if (++count % 1_000_000 == 0) {
            log.debug("Processed " + count + " reads.");
          }
        }
      }
    } finally {
      for (IntervalFileWriter<Interval> writer : writers) {
        writer.close();
      }
    }
  }

  public static void main(String[] args) {
    new SplitReads().instanceMain(args);
  }
}