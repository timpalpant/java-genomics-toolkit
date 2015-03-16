package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Average multiple Wig files base pair by base pair
 * 
 * @author timpalpant
 *
 */
public class Average extends WigMathTool {

  private static final Logger log = Logger.getLogger(Average.class);

  @Parameter(description = "Input files", required = true)
  public List<String> inputFiles = new ArrayList<String>();

  @Override
  public void setup() {
    if (inputFiles.size() < 2) {
      throw new CommandLineToolException("No reason to average < 2 files.");
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
    float[] avg = new float[chunk.length()];
    int[] count = new int[chunk.length()];

    for (WigFileReader wig : inputs) {
      float[] data = wig.query(chunk).getValues();
      for (int i = 0; i < data.length; i++) {
        if (!Float.isNaN(data[i])) {
          avg[i] += data[i];
          count[i]++;
        }
      }
    }

    for (int i = 0; i < avg.length; i++) {
      if (count[i] > 0) {
        avg[i] /= count[i];
      } else {
        avg[i] = Float.NaN;
      }
    }

    return avg;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new Average().instanceMain(args);
  }

}
