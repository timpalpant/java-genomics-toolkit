package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Z-score the values in a Wig file: calculate normal scores =
 * (value-mean)/stdev
 * 
 * @author timpalpant
 *
 */
public class ZScore extends WigMathTool {

  private static final Logger log = Logger.getLogger(ZScore.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-b", "--bychr" }, description = "Z-score each chromosome individually")
  public boolean byChromosome = false;

  WigFileReader reader;
  Map<String, Float> means = new HashMap<>();
  Map<String, Float> stdevs = new HashMap<>();

  @Override
  public void setup() {
    try {
      reader = WigFileReader.autodetect(inputFile);
      for (String chr : reader.chromosomes()) {
        float mean, stdev;
        if (byChromosome) {
          SummaryStatistics stats = reader.queryStats(chr, reader.getChrStart(chr), reader.getChrStop(chr));
          mean = (float) stats.getMean();
          stdev = (float) stats.getStandardDeviation();
          log.debug("Z-scoring " + chr + " to chromosome mean = " + mean + ", stdev = " + stdev);
        } else {
          mean = (float) reader.mean();
          stdev = (float) reader.stdev();
        }

        if (stdev == 0) {
          throw new CommandLineToolException("Cannot Z-score a file with stdev = 0!");
        }
        means.put(chr, mean);
        stdevs.put(chr, stdev);
      }
    } catch (IOException | WigFileException e) {
      throw new CommandLineToolException(e);
    }
    inputs.add(reader);

    if (!byChromosome) {
      log.debug("Z-scoring all chromosomes to global mean = " + reader.mean() + ", stdev = " + reader.stdev());
    }
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    float[] result = reader.query(chunk).getValues();
    float mean = means.get(chunk.getChr());
    float stdev = stdevs.get(chunk.getChr());
    for (int i = 0; i < result.length; i++) {
      result[i] = (result[i] - mean) / stdev;
    }

    return result;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new ZScore().instanceMain(args);
  }

}
