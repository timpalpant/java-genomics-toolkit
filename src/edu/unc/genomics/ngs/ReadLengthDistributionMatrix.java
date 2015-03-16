package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * Creates a heatmap matrix of sequencing read coverage separated by read length
 * See Floer M, et al. (2010) A RSC/nucleosome complex determines chromatin
 * architecture and facilitates activator binding. Cell 141: 407-418 for
 * examples.
 * 
 * @author timpalpant
 *
 */
public class ReadLengthDistributionMatrix extends CommandLineTool {

  private static final Logger log = Logger.getLogger(ReadLengthDistributionMatrix.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (reads)", required = true, validateWith = ReadablePathValidator.class)
  public Path intervalFile;
  @Parameter(names = { "-c", "--chr" }, description = "Chromosome", required = true)
  public String chr;
  @Parameter(names = { "-s", "--start" }, description = "Start base pair", required = true)
  public int start;
  @Parameter(names = { "-e", "--stop" }, description = "Stop base pair", required = true)
  public int stop;
  @Parameter(names = { "-m", "--min" }, description = "Minimum fragment length bin (bp)")
  public int min = 1;
  @Parameter(names = { "-l", "--max" }, description = "Maximum fragment length bin (bp)")
  public int max = 200;
  @Parameter(names = { "-b", "--bin" }, description = "Bin size (bp)")
  public int binSize = 1;
  @Parameter(names = { "-o", "--output" }, description = "Matrix output file (tabular)", required = true)
  public Path outputFile;
  @Parameter(names = { "-p", "--pileup" }, description = "Pileup output file (tabular)")
  public Path pileupFile;

  @Override
  public void run() throws IOException {
    int regionLength = stop - start + 1;
    int lengthRange = max - min + 1;
    int histLength = lengthRange / binSize;
    if (histLength * binSize != lengthRange) {
      histLength++;
    }

    log.debug("Binning reads by genomic location and length");
    int[][] counts = new int[histLength][regionLength];
    int[] pileup = new int[regionLength];
    int skipped = 0;
    try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(intervalFile)) {
      Iterator<? extends Interval> reads = reader.query(chr, start, stop);
      while (reads.hasNext()) {
        Interval read = reads.next();
        if (read.length() < min || read.length() > max) {
          skipped++;
          continue;
        }
        int bin = (read.length() - min) / binSize;
        int intersectStart = Math.max(read.getStart(), start);
        int intersectStop = Math.min(read.getStop(), stop);
        for (int i = intersectStart; i <= intersectStop; i++) {
          counts[bin][i - start]++;
          pileup[i - start]++;
        }
      }
    }

    log.info("Skipped " + skipped + " reads with length outside range");

    // Write to output in matrix2png format
    try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      // Header line
      writer.write(chr);
      for (int bp = start; bp <= stop; bp++) {
        writer.write("\t" + bp);
      }

      for (int i = histLength - 1; i >= 0; i--) {
        writer.newLine();
        writer.write(String.valueOf(min + i * binSize));
        for (int j = 0; j < regionLength; j++) {
          writer.write("\t" + counts[i][j]);
        }
      }
    }

    // Write to output in DataGraph format
    if (pileupFile != null) {
      try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(pileupFile, Charset.defaultCharset()))) {
        // Header line
        writer.println("Pos\tValue");

        for (int i = 0; i < regionLength; i++) {
          writer.println((start + i) + "\t" + pileup[i]);
        }
      }
    }
  }

  public static void main(String[] args) {
    new ReadLengthDistributionMatrix().instanceMain(args);
  }
}