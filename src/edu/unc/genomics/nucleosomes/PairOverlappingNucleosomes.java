package edu.unc.genomics.nucleosomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.NucleosomeCall;
import edu.unc.genomics.NucleosomeCallsFileReader;
import edu.unc.genomics.ReadablePathValidator;

/**
 * Takes two sets of nucleosome calls and pairs them with the most likely
 * overlapping call
 * 
 * @author timpalpant
 *
 */
public class PairOverlappingNucleosomes extends CommandLineTool {

  private static final Logger log = Logger.getLogger(PairOverlappingNucleosomes.class);

  @Parameter(names = { "-a", "--input1" }, description = "Input file 1 (nucleosome calls)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile1;
  @Parameter(names = { "-b", "--input2" }, description = "Input file 2 (nucleosome calls)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile2;
  @Parameter(names = { "-m", "--overlap" }, description = "Minimum overlap (bp)")
  public int minOverlap = 73;
  @Parameter(names = { "-o", "--output" }, description = "Paired overlapping calls", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    if (minOverlap <= 0) {
      throw new ParameterException("Invalid overlap! Must be > 0");
    }

    int paired = 0;
    try (NucleosomeCallsFileReader nucReader1 = new NucleosomeCallsFileReader(inputFile1);
        NucleosomeCallsFileReader nucReader2 = new NucleosomeCallsFileReader(inputFile2);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      // Write header
      writer
          .write("#chr\tstart\tstop\tdyad\tdyadStdev\tdyadMean\toccupancy\tchr\tstart\tstop\tdyad\tdyadStdev\tdyadMean\toccupancy");
      writer.newLine();

      for (NucleosomeCall call1 : nucReader1) {
        // Find the best overlapping call
        int maxOverlap = 0;
        NucleosomeCall mate = null;
        Iterator<NucleosomeCall> overlappingNucs = nucReader2.query(call1);
        while (overlappingNucs.hasNext()) {
          NucleosomeCall call2 = overlappingNucs.next();
          int overlap = Math.min(call1.high(), call2.high()) - Math.max(call1.low(), call2.low());
          if (overlap > maxOverlap) {
            maxOverlap = overlap;
            mate = call2;
          }
        }

        // Found a pair, write to output
        if (maxOverlap > minOverlap) {
          paired++;
          writer.write(call1.getChr() + "\t" + call1.getStart() + "\t" + call1.getStop() + "\t" + call1.getDyad()
              + "\t" + call1.getDyadStdev() + "\t" + call1.getDyadMean() + "\t" + call1.occupancy());
          writer.write("\t" + mate.getChr() + "\t" + mate.getStart() + "\t" + mate.getStop() + "\t" + mate.getDyad()
              + "\t" + mate.getDyadStdev() + "\t" + mate.getDyadMean() + "\t" + mate.occupancy());
          writer.newLine();
        }
      }
    }

    log.info("Found " + paired + " paired nucleosomes");
  }

  public static void main(String[] args) {
    new PairOverlappingNucleosomes().instanceMain(args);
  }
}