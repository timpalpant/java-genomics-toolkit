package edu.unc.genomics.converters;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.BedFileWriter;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * Convert any known interval format to Bed-6 format
 * 
 * @author timpalpant
 *
 */
public class IntervalToBed extends CommandLineTool {

  private static final Logger log = Logger.getLogger(IntervalToBed.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (Bedgraph/BigBed/GFF/SAM/BAM format)", required = true)
  public Path inputFile;
  @Parameter(names = { "-o", "--output" }, description = "Output file (Bed format)", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    log.debug("Initializing input/output files");
    try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(inputFile);
        BedFileWriter<Interval> writer = new BedFileWriter<>(outputFile)) {
      for (Interval entry : reader) {
        writer.write(entry);
      }
    }
  }

  public static void main(String[] args) {
    new IntervalToBed().instanceMain(args);
  }

}
