package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Shift a Wig file to have a specified mean
 * 
 * @author timpalpant
 *
 */
public class Shift extends WigMathTool {

  private static final Logger log = Logger.getLogger(Shift.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-m", "--mean" }, description = "New mean")
  public float newMean = 0;
  @Parameter(names = { "-b", "--bychr" }, description = "Shift each chromosome individually")
  public boolean byChromosome = false;

  WigFileReader reader;
  Map<String, Float> shifts = new HashMap<>();

  @Override
  public void setup() {
    try {
      reader = WigFileReader.autodetect(inputFile);
      for (String chr : reader.chromosomes()) {
        float shift;
        if (byChromosome) {
          float chrMean = (float) reader.queryStats(chr, reader.getChrStart(chr), reader.getChrStop(chr)).getMean();
          log.debug("Mean of " + chr + " = " + chrMean);
          shift = newMean - chrMean;
        } else {
          shift = (float) (newMean - reader.mean());
        }
        shifts.put(chr, shift);
      }
    } catch (IOException | WigFileException e) {
      throw new CommandLineToolException(e);
    }
    inputs.add(reader);
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    float[] result = reader.query(chunk).getValues();
    float shift = shifts.get(chunk.getChr());
    for (int i = 0; i < result.length; i++) {
      result[i] += shift;
    }

    return result;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new Shift().instanceMain(args);
  }

}
