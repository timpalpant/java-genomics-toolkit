package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Multiply (Big)Wig files base pair by base pair
 * 
 * @author timpalpant
 *
 */
public class Multiply extends WigMathTool {

  private static final Logger log = Logger.getLogger(Multiply.class);

  @Parameter(description = "Input files", required = true)
  public List<String> inputFiles = new ArrayList<String>();

  @Override
  public void setup() {
    log.debug("Initializing input files");
    for (String inputFile : inputFiles) {
      try {
        addInputFile(WigFileReader.autodetect(Paths.get(inputFile)));
      } catch (IOException e) {
        throw new CommandLineToolException(e);
      }
    }
    log.debug("Initialized " + inputs.size() + " input files");
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    float[] product = new float[chunk.length()];
    Arrays.fill(product, 1);

    for (WigFileReader wig : inputs) {
      float[] data = wig.query(chunk).getValues();
      for (int i = 0; i < data.length; i++) {
        if (!Float.isNaN(data[i])) {
          product[i] *= data[i];
        }
      }
    }

    return product;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new Multiply().instanceMain(args);
  }

}
