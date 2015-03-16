package edu.unc.genomics.nucleosomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.NucleosomeCall;
import edu.unc.genomics.NucleosomeCallsFileReader;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * Gets the dyad coordinate of the first and last (5' and 3') nucleosome for
 * each interval
 * 
 * @author timpalpant
 *
 */
public class FindBoundaryNucleosomes extends CommandLineTool {

  private static final Logger log = Logger.getLogger(FindBoundaryNucleosomes.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (nucleosome calls)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-l", "--loci" }, description = "Intervals (Bed format)", required = true, validateWith = ReadablePathValidator.class)
  public Path lociFile;
  @Parameter(names = { "-o", "--output" }, description = "Output file", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    log.debug("Initializing output file");
    int skipped = 0;
    try (NucleosomeCallsFileReader nucReader = new NucleosomeCallsFileReader(inputFile);
        IntervalFileReader<? extends Interval> lociReader = IntervalFileReader.autodetect(lociFile);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      // Write header
      writer.write("#chr\tlow\thigh\tid\talignment\tstrand\tlow boundary dyad\thigh boundary dyad");
      writer.newLine();

      log.debug("Finding boundary nucleosomes for each interval");
      NucleosomeCall.DyadComparator comparator = new NucleosomeCall.DyadComparator();
      for (Interval interval : lociReader) {
        writer.write(interval.toBed());

        // Get all of the nucleosomes within this interval
        List<NucleosomeCall> intervalNucs = nucReader.load(interval);

        if (intervalNucs.size() > 0) {
          // Sort the list by nucleosome position
          Collections.sort(intervalNucs, comparator);
          int low = intervalNucs.get(0).getDyad();
          int high = intervalNucs.get(intervalNucs.size() - 1).getDyad();
          writer.write("\t" + low + "\t" + high);
        } else {
          skipped++;
          writer.write("\tNA\tNA");
        }

        writer.newLine();
      }
    }

    log.info("Skipped " + skipped + " intervals with 0 nucleosomes");
  }

  public static void main(String[] args) {
    new FindBoundaryNucleosomes().instanceMain(args);
  }
}