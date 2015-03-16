package edu.unc.genomics.nucleosomes;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Make a histogram of phase counts from sequencing data to identify
 * periodicities. See Valouev A, et al. (2011) Determinants of nucleosome
 * organization in primary human cells. Nature 474: 516-520
 * 
 * @author timpalpant
 *
 */
public class Phasogram extends CommandLineTool {

  private static final Logger log = Logger.getLogger(Phasogram.class);

  @Parameter(names = { "-i", "--input" }, description = "Input wig file (read counts)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-m", "--max" }, description = "Maximum phase shift", required = true)
  public int maxPhase;
  @Parameter(names = { "-o", "--output" }, description = "Output file (histogram)", required = true)
  public Path outputFile;

  public void run() throws IOException {
    double[] phaseCounts = new double[maxPhase + 1];

    // Process each chromosome in the input file
    try (WigFileReader reader = WigFileReader.autodetect(inputFile)) {
      for (String chr : reader.chromosomes()) {
        log.debug("Processing chromosome " + chr);
        int start = reader.getChrStart(chr);
        while (start < reader.getChrStop(chr)) {
          int stop = Math.min(start + DEFAULT_CHUNK_SIZE - 1, reader.getChrStop(chr));
          log.debug("Processing chunk " + chr + ":" + start + "-" + stop);
          int paddedStop = Math.min(stop + maxPhase, reader.getChrStop(chr));

          try {
            float[] data = reader.query(chr, start, paddedStop).getValues();
            for (int i = 0; i < data.length - maxPhase; i++) {
              if (!Float.isNaN(data[i]) && !Float.isInfinite(data[i])) {
                for (int j = 0; j <= maxPhase; j++) {
                  if (!Float.isNaN(data[i + j]) && !Float.isInfinite(data[i + j])) {
                    phaseCounts[j] += data[i] * data[i + j];
                  }
                }
              }
            }
          } catch (WigFileException e) {
            log.fatal("Error querying data from Wig file!");
            e.printStackTrace();
            throw new CommandLineToolException("Error querying data from Wig file!");
          }

          // Process the next chunk
          start = stop + 1;
        }
      }
    }

    log.debug("Writing output to disk");
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, Charset.defaultCharset()))) {
      writer.println("#Phase\tCount");
      for (int i = 0; i < phaseCounts.length; i++) {
        writer.println(i + "\t" + phaseCounts[i]);
      }
    }
  }

  public static void main(String[] args) {
    new Phasogram().instanceMain(args);
  }
}