package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.ArrayUtils;

/**
 * For a set of intervals, this tool finds the location of the largest value in
 * a Wig file for each interval.
 * 
 * @author timpalpant
 *
 */
public class FindAbsoluteMaxima extends CommandLineTool {

  private static final Logger log = Logger.getLogger(FindAbsoluteMaxima.class);

  @Parameter(description = "Input files", required = true)
  public List<String> inputFiles = new ArrayList<String>();
  @Parameter(names = { "-l", "--loci" }, description = "Loci file (Bed)", required = true, validateWith = ReadablePathValidator.class)
  public Path lociFile;
  @Parameter(names = { "-o", "--output" }, description = "Output file", required = true)
  public Path outputFile;

  private List<WigFileReader> wigs = new ArrayList<>();

  @Override
  public void run() throws IOException {
    log.debug("Initializing input Wig file(s)");
    for (String inputFile : inputFiles) {
      wigs.add(WigFileReader.autodetect(Paths.get(inputFile)));
    }

    log.debug("Initializing output file");
    int count = 0, skipped = 0;
    try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(lociFile);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      writer.write("#Chr\tStart\tStop\tID\tValue\tStrand");
      for (String inputFile : inputFiles) {
        Path p = Paths.get(inputFile);
        writer.write("\t" + p.getFileName().toString());
      }
      writer.newLine();

      log.debug("Iterating over all intervals and finding maxima");
      for (Interval interval : reader) {
        writer.write(interval.toBed());
        for (WigFileReader wig : wigs) {
          try {
            float[] data = wig.query(interval).getValues();
            int dir = interval.isWatson() ? 1 : -1;
            int maxima = interval.getStart() + dir * ArrayUtils.maxIndex(data);
            writer.write("\t" + maxima);
          } catch (WigFileException e) {
            writer.write("\t" + Float.NaN);
            skipped++;
          }
        }
        writer.newLine();
        count++;
      }
    }

    for (WigFileReader wig : wigs) {
      wig.close();
    }

    log.info(count + " intervals processed");
    log.info(skipped + " interval skipped");
  }

  public static void main(String[] args) {
    new FindAbsoluteMaxima().instanceMain(args);
  }

}
