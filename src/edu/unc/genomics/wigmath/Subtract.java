package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Subtract two (Big)Wig files
 * 
 * @author timpalpant
 *
 */
public class Subtract extends WigMathTool {

  private static final Logger log = Logger.getLogger(Subtract.class);

  @Parameter(names = { "-m", "--minuend" }, description = "Minuend (top - file 1)", required = true, validateWith = ReadablePathValidator.class)
  public Path minuendFile;
  @Parameter(names = { "-s", "--subtrahend" }, description = "Subtrahend (bottom - file 2)", required = true, validateWith = ReadablePathValidator.class)
  public Path subtrahendFile;
  @Parameter(names = { "-z", "--assume-zero" }, description = "Assume missing data is zero")
  public boolean assumeZero = false;

  WigFileReader minuendReader, subtrahendReader;

  @Override
  public void setup() {
    log.debug("Initializing input files");
    try {
      minuendReader = WigFileReader.autodetect(minuendFile);
      subtrahendReader = WigFileReader.autodetect(subtrahendFile);
    } catch (IOException e) {
      throw new CommandLineToolException(e);
    }
    inputs.add(minuendReader);
    inputs.add(subtrahendReader);
    log.debug("Initialized " + inputs.size() + " input files");
    if (assumeZero) {
      log.debug("Assuming missing data is zero");
      unionExtents = assumeZero;
    }
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    float[] minuend = minuendReader.query(chunk).getValues();
    float[] subtrahend = subtrahendReader.query(chunk).getValues();

    // Fill missing data with zeros
    if (assumeZero) {
      for (int i = 0; i < minuend.length; i++) {
        if (Float.isNaN(minuend[i])) {
          minuend[i] = 0;
        }
        if (Float.isNaN(subtrahend[i])) {
          subtrahend[i] = 0;
        }
      }
    }

    for (int i = 0; i < minuend.length; i++) {
      minuend[i] -= subtrahend[i];
    }

    return minuend;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new Subtract().instanceMain(args);
  }

}
