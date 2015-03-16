package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.commons.math3.stat.Frequency;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * Calculate the fragment length distribution for fragments overlapping each
 * gene
 * 
 * @author timpalpant
 *
 */
public class FragmentLengthDistributionByGene extends CommandLineTool {

  private static final Logger log = Logger.getLogger(FragmentLengthDistributionByGene.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (reads)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-l", "--loci" }, description = "Loci file (Bed)", required = true, validateWith = ReadablePathValidator.class)
  public Path lociFile;
  @Parameter(names = { "-m", "min" }, description = "Minimum read length to count")
  public int min = 1;
  @Parameter(names = { "-h", "--max" }, description = "Maximum read length to count")
  public int max = 1000;
  @Parameter(names = { "-o", "--output" }, description = "Output file", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    Frequency hist = new Frequency();
    try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(inputFile);
        IntervalFileReader<? extends Interval> loci = IntervalFileReader.autodetect(lociFile);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      writer.write("#Chr\tStart\tStop\tID\tAlignment\tStrand\tDistribution");
      writer.newLine();

      log.debug("Iterating over all intervals and computing read length distribution");
      for (Interval interval : loci) {
        writer.write(interval.toBed());

        // Make a histogram of all reads that overlap this interval
        hist.clear();
        Iterator<? extends Interval> reads = reader.query(interval);
        while (reads.hasNext()) {
          Interval read = reads.next();
          hist.addValue(read.length());
        }

        for (int i = min; i <= max; i++) {
          writer.write("\t" + hist.getPct(i));
        }
        writer.newLine();
      }
    }
  }

  public static void main(String[] args) {
    new FragmentLengthDistributionByGene().instanceMain(args);
  }

}
