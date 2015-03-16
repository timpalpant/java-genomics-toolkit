package edu.unc.genomics.visualization;

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

import edu.unc.genomics.BedEntry;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.BedFileReader;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Aggregate and average genomic signal for a set of aligned intervals
 * 
 * @author timpalpant
 *
 */
public class IntervalAverager extends CommandLineTool {

  private static final Logger log = Logger.getLogger(IntervalAverager.class);

  @Parameter(description = "Input files", required = true)
  public List<String> inputFiles = new ArrayList<String>();
  @Parameter(names = { "-l", "--loci" }, description = "Loci file (Bed)", required = true, validateWith = ReadablePathValidator.class)
  public Path lociFile;
  @Parameter(names = { "-o", "--output" }, description = "Output file (matrix2png format)", required = true)
  public Path outputFile;

  private List<WigFileReader> wigs = new ArrayList<>();
  private int numFiles;
  private List<BedEntry> loci;

  @Override
  public void run() throws IOException {
    log.debug("Initializing input files");
    for (String inputFile : inputFiles) {
      try {
        WigFileReader wig = WigFileReader.autodetect(Paths.get(inputFile));
        wigs.add(wig);
      } catch (IOException e) {
        log.error("IOError initializing input Wig file: " + inputFile);
        e.printStackTrace();
        throw new CommandLineToolException(e.getMessage());
      }
    }
    numFiles = wigs.size();
    log.debug("Initialized " + numFiles + " input files");

    log.debug("Loading alignment intervals");
    try (BedFileReader bed = new BedFileReader(lociFile)) {
      loci = bed.loadAll();
    }

    // Compute the matrix dimensions
    int leftMax = Integer.MIN_VALUE;
    int rightMax = Integer.MIN_VALUE;
    for (BedEntry entry : loci) {
      if (entry.getValue() == null) {
        throw new CommandLineToolException("You must specify an alignment point for each interval in column 5");
      }
      int left = Math.abs(entry.getValue().intValue() - entry.getStart());
      int right = Math.abs(entry.getValue().intValue() - entry.getStop());
      if (left > leftMax) {
        leftMax = left;
      }
      if (right > rightMax) {
        rightMax = right;
      }
    }

    int m = loci.size();
    int n = leftMax + rightMax + 1;
    int alignmentPoint = leftMax;
    log.info("Intervals aligned into: " + m + "x" + n + " matrix");
    log.info("Alignment point: " + alignmentPoint);

    float[][] sum = new float[numFiles][n];
    int[][] counts = new int[numFiles][n];
    int count = 0, skipped = 0;
    log.debug("Iterating over all intervals");
    for (BedEntry entry : loci) {
      // Locus alignment point (entry value) should be positioned over the
      // global alignment point
      int n1 = alignmentPoint - Math.abs(entry.getValue().intValue() - entry.getStart());
      int n2 = alignmentPoint + Math.abs(entry.getValue().intValue() - entry.getStop());

      for (int i = 0; i < numFiles; i++) {
        WigFileReader w = wigs.get(i);
        try {
          float[] data = w.query(entry).getValues();
          assert data.length == n2 - n1 + 1;
          for (int bp = n1; bp <= n2; bp++) {
            if (!Float.isNaN(data[bp - n1]) && !Float.isInfinite(data[bp - n1])) {
              sum[i][bp] += data[bp - n1];
              counts[i][bp]++;
            }
          }
        } catch (WigFileException e) {
          log.debug("Error getting data from wig file " + w.getPath().getFileName() + " for interval " + entry);
          skipped++;
        }
      }

      count++;
    }
    log.info(count + " intervals processed");
    log.info(skipped + " intervals skipped");

    log.debug("Computing average(s)");
    float[][] avg = new float[numFiles][n];
    for (int i = 0; i < numFiles; i++) {
      for (int j = 0; j < n; j++) {
        if (counts[i][j] == 0) {
          avg[i][j] = Float.NaN;
        } else {
          avg[i][j] = sum[i][j] / counts[i][j];
        }
      }
    }

    log.debug("Writing average profile(s) to output");
    try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      // Header
      writer.write("Position");
      for (WigFileReader w : wigs) {
        writer.write("\t" + w.getPath().getFileName());
      }
      writer.newLine();

      // Average values
      for (int i = 0; i < n; i++) {
        writer.write(String.valueOf(i - alignmentPoint));
        for (int j = 0; j < numFiles; j++) {
          writer.write("\t" + avg[j][i]);
        }
        writer.newLine();
      }
    }

    // Close the input files
    for (WigFileReader w : wigs) {
      w.close();
    }
  }

  public static void main(String[] args) {
    new IntervalAverager().instanceMain(args);
  }
}