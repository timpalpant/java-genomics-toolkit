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
 * Divide two (Big)Wig files base pair by base pair
 * 
 * @author timpalpant
 *
 */
public class Divide extends WigMathTool {

  private static final Logger log = Logger.getLogger(Divide.class);

  @Parameter(names = { "-n", "--numerator" }, description = "Dividend / Numerator (file 1)", required = true, validateWith = ReadablePathValidator.class)
  public Path dividendFile;
  @Parameter(names = { "-d", "--denominator" }, description = "Divisor / Denominator (file 2)", required = true)
  public Path divisorFile;

  WigFileReader dividendReader, divisorReader;

  @Override
  public void setup() {
    try {
      dividendReader = WigFileReader.autodetect(dividendFile);
      divisorReader = WigFileReader.autodetect(divisorFile);
    } catch (IOException e) {
      throw new CommandLineToolException(e);
    }
    inputs.add(dividendReader);
    inputs.add(divisorReader);
    log.debug("Initialized " + inputs.size() + " input files");
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    float[] dividend = dividendReader.query(chunk).getValues();
    float[] divisor = divisorReader.query(chunk).getValues();
    for (int i = 0; i < dividend.length; i++) {
      if (divisor[i] == 0) {
        dividend[i] = Float.NaN;
      } else {
        dividend[i] /= divisor[i];
      }
    }

    return dividend;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new Divide().instanceMain(args);
  }

}
