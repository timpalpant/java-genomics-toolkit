package edu.unc.genomics.wigmath;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.WigAnalysisTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Single base pair Pearson correlation of multiple (Big)Wig files
 * 
 * @author timpalpant
 *
 */
public class Correlate extends WigAnalysisTool {

  private static final Logger log = Logger.getLogger(Correlate.class);

  @Parameter(description = "Input files", required = true)
  public List<String> inputFiles = new ArrayList<String>();
  @Parameter(names = { "-z", "--assume-zero" }, description = "Assume missing data is zero")
  public boolean assumeZero = false;
  @Parameter(names = { "-o", "--output" }, description = "Output file")
  public Path outputFile;

  /**
   * Number of shared data points for each pair of input files
   */
  private int[][] n;
  /**
   * Partial sums for each pair of input files
   */
  private double[][] sumX, sumY, sumSqX, sumSqY, sumXY;

  @Override
  protected void prepare() {
    if (inputFiles.size() < 2) {
      throw new CommandLineToolException("Cannot correlate < 2 input files.");
    }

    log.debug("Initializing input files");
    for (String inputFile : inputFiles) {
      try {
        addInputFile(WigFileReader.autodetect(Paths.get(inputFile)));
      } catch (IOException e) {
        throw new CommandLineToolException("IOError initializing input Wig file: " + inputFile, e);
      }
    }
    log.debug("Initialized " + inputs.size() + " input files");

    // Consider the union of all input files
    // bases will be skipped when data is missing for one strain (NaN)
    unionExtents = true;

    n = new int[inputs.size()][inputs.size()];
    sumX = new double[inputs.size()][inputs.size()];
    sumY = new double[inputs.size()][inputs.size()];
    sumSqX = new double[inputs.size()][inputs.size()];
    sumSqY = new double[inputs.size()][inputs.size()];
    sumXY = new double[inputs.size()][inputs.size()];
  }

  @Override
  protected void process(Interval chunk) throws IOException, WigFileException {
    // Calculate the partial sums for correlation b/w all pairs
    float[][] values = new float[inputs.size()][chunk.length()];
    for (int i = 0; i < inputs.size(); i++) {
      // Load the data for Wig file i
      values[i] = inputs.get(i).query(chunk).getValues();
      if (assumeZero) {
        for (int k = 0; k < chunk.length(); k++) {
          if (Float.isNaN(values[i][k])) {
            values[i][k] = 0;
          }
        }
      }

      // Compute the partial sums for correlation with all
      // data files < i (the data has already been loaded)
      for (int j = 0; j < i; j++) {
        for (int k = 0; k < chunk.length(); k++) {
          float x = values[i][k];
          float y = values[j][k];
          if (!Float.isNaN(x) && !Float.isInfinite(x) && !Float.isNaN(y) && !Float.isInfinite(y)) {
            n[i][j]++;
            sumX[i][j] += x;
            sumY[i][j] += y;
            sumSqX[i][j] += x * x;
            sumSqY[i][j] += y * y;
            sumXY[i][j] += x * y;
          }
        }
      }
    }
  }

  @Override
  protected void shutdown() throws IOException {
    // Calculate the correlation between each pair
    float[][] correlation = new float[inputs.size()][inputs.size()];

    // Calculate the correlations
    for (int i = 0; i < correlation.length; i++) {
      // Put ones down the diagonal
      correlation[i][i] = 1;

      for (int j = 0; j < i; j++) {
        double covarXY = n[i][j] * sumXY[i][j] - sumX[i][j] * sumY[i][j];
        double stdX = Math.sqrt(n[i][j] * sumSqX[i][j] - sumX[i][j] * sumX[i][j]);
        double stdY = Math.sqrt(n[i][j] * sumSqY[i][j] - sumY[i][j] * sumY[i][j]);
        correlation[i][j] = (float) (covarXY / (stdX * stdY));
        // Symmetrize the correlation matrix
        correlation[j][i] = correlation[i][j];
      }
    }

    printCorrelationMatrix(correlation);
  }

  private void printCorrelationMatrix(float[][] correlation) throws IOException {
    // Write the correlation matrix to output
    StringBuilder output = new StringBuilder();
    // Header row
    for (int i = 0; i < inputs.size(); i++) {
      output.append("\t" + inputs.get(i).getPath().getFileName());
    }
    // Data rows
    for (int i = 0; i < inputs.size(); i++) {
      output.append("\n" + inputs.get(i).getPath().getFileName());
      for (int j = 0; j < inputs.size(); j++) {
        output.append("\t" + correlation[i][j]);
      }
    }

    if (outputFile != null) {
      log.debug("Writing to output file");
      try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
        writer.write(output.toString());
      }
    } else {
      System.out.println(output.toString());
    }
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new Correlate().instanceMain(args);
  }

}
