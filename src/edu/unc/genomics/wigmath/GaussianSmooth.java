package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Smooth a Wig file with a Gaussian filter
 * 
 * @author timpalpant
 *
 */
public class GaussianSmooth extends WigMathTool {

  private static final Logger log = Logger.getLogger(GaussianSmooth.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-s", "--stdev" }, description = "Standard deviation of Gaussian (bp)")
  public int stdev = 20;
  @Parameter(names = { "-w", "--window" }, description = "Kernel width in (+/-) standard deviations")
  public int windowWidth = 3;

  WigFileReader reader;
  float[] filter;

  @Override
  public void setup() {
    try {
      reader = WigFileReader.autodetect(inputFile);
    } catch (IOException e) {
      throw new CommandLineToolException(e);
    }
    inputs.add(reader);

    // Use a window size equal to +/- SD's
    log.debug("Initializing Gaussian filter");
    filter = new float[2 * windowWidth * stdev + 1];
    float sum = 0;
    for (int i = 0; i < filter.length; i++) {
      float x = i - 3 * stdev;
      float value = (float) Math.exp(-(x * x) / (2 * stdev * stdev));
      filter[i] = value;
      sum += value;
    }
    // Normalize so that the filter is area-preserving (has total area = 1)
    for (int i = 0; i < filter.length; i++) {
      filter[i] /= sum;
    }
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    // Pad the query for smoothing
    int paddedStart = Math.max(chunk.getStart() - windowWidth * stdev, reader.getChrStart(chunk.getChr()));
    int paddedStop = Math.min(chunk.getStop() + windowWidth * stdev, reader.getChrStop(chunk.getChr()));
    Contig result = reader.query(chunk.getChr(), paddedStart, paddedStop);
    float[] data = result.get(chunk.getStart() - windowWidth * stdev, chunk.getStop() + windowWidth * stdev);

    // Convolve the data with the filter
    float[] smoothed = new float[chunk.length()];
    for (int i = 0; i < smoothed.length; i++) {
      for (int j = 0; j < filter.length; j++) {
        smoothed[i] += data[i + j] * filter[j];
      }
    }

    return smoothed;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new GaussianSmooth().instanceMain(args);
  }

}
