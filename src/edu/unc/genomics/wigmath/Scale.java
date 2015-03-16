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
 * Scale a Wig file by a constant, or normalize to its mean value
 * 
 * @author timpalpant
 *
 */
public class Scale extends WigMathTool {

  private static final Logger log = Logger.getLogger(Scale.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-m", "--multiplier" }, description = "Multiplier (scale factor, default = 1/mean)")
  public Float multiplier;
  @Parameter(names = { "-b", "--bychr" }, description = "Scale to the mean of each chromosome")
  public boolean byChromosome = false;

  WigFileReader reader;
  Map<String, Float> scales = new HashMap<>();

  @Override
  public void setup() {
    try {
      reader = WigFileReader.autodetect(inputFile);

      for (String chr : reader.chromosomes()) {
        float scale;
        if (multiplier == null || multiplier == 0) {
          if (byChromosome) {
            SummaryStatistics stats = reader.queryStats(chr, reader.getChrStart(chr), reader.getChrStop(chr));
            scale = (float) (stats.getN() / stats.getSum());
            log.debug("Scaling " + chr + " to chromosome mean: " + stats.getMean());
          } else {
            scale = (float) (reader.numBases() / reader.total());
            log.debug("Scaling " + chr + " to global mean: " + reader.mean());
          }
        } else {
          scale = multiplier;
          log.debug("Scaling " + chr + " by " + multiplier);
        }
        scales.put(chr, scale);
      }

    } catch (IOException | WigFileException e) {
      throw new CommandLineToolException(e);
    }
    inputs.add(reader);
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    float[] result = reader.query(chunk).getValues();
    float scale = scales.get(chunk.getChr());
    for (int i = 0; i < result.length; i++) {
      result[i] *= scale;
    }

    return result;
  }

  public static void main(String[] args) throws IOException, WigFileException {
    new Scale().instanceMain(args);
  }

}
