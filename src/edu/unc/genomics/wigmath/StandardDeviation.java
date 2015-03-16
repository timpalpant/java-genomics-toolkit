package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Calculate base pair by base pair variance for a set of Wig files
 * 
 * @author timpalpant
 *
 */
public class StandardDeviation extends WigMathTool {

  private static final Logger log = Logger.getLogger(StandardDeviation.class);

  @Parameter(description = "Input files", required = true)
  public List<String> inputFiles = new ArrayList<String>();

  @Override
  public void setup() {
    if (inputFiles.size() < 2) {
      throw new CommandLineToolException("Cannot compute variance with < 2 files.");
    }

    log.debug("Initializing input files");
    for (String inputFile : inputFiles) {
      try {
        addInputFile(WigFileReader.autodetect(Paths.get(inputFile)));
      } catch (IOException e) {
        log.error("IOError initializing input Wig file: " + inputFile);
        e.printStackTrace();
        throw new CommandLineToolException(e.getMessage());
      }
    }
    log.debug("Initialized " + inputs.size() + " input files");
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    SummaryStatistics[] stats = new SummaryStatistics[chunk.length()];
    for (int i = 0; i < stats.length; i++) {
      stats[i] = new SummaryStatistics();
    }

    for (WigFileReader wig : inputs) {
      float[] data = wig.query(chunk).getValues();
      for (int i = 0; i < data.length; i++) {
        if (!Float.isNaN(data[i])) {
          stats[i].addValue(data[i]);
        }
      }
    }

    float[] result = new float[chunk.length()];
    for (int i = 0; i < result.length; i++) {
      result[i] = (float) stats[i].getStandardDeviation();
    }
    return result;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new StandardDeviation().instanceMain(args);
  }

}
