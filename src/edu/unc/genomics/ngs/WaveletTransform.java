package edu.unc.genomics.ngs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.ArrayScaler;

/**
 * This tool performs a Wavelet scaling analysis on data from a genomic interval
 * 
 * @author timpalpant
 *
 */
public class WaveletTransform extends CommandLineTool {

  private static final Logger log = Logger.getLogger(WaveletTransform.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (Wig)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-w", "--wavelet" }, description = "Orthonormal wavelet function (txt)", required = true, validateWith = ReadablePathValidator.class)
  public Path waveletFile;
  @Parameter(names = { "-c", "--chr" }, description = "Chromosome", required = true)
  public String chr;
  @Parameter(names = { "-s", "--start" }, description = "Start base pair", required = true)
  public int start;
  @Parameter(names = { "-e", "--stop" }, description = "Stop base pair", required = true)
  public int stop;
  @Parameter(names = { "-m", "--min" }, description = "Minimum wavelet size (bp)")
  public int minLength = 100;
  @Parameter(names = { "-l", "--max" }, description = "Maximum wavelet size (bp)")
  public int maxLength = 200;
  @Parameter(names = { "-n", "--step" }, description = "Step size (bp)")
  public int stepSize = 1;
  @Parameter(names = { "-o", "--output" }, description = "Output file (tabular)", required = true)
  public Path outputFile;

  private ArrayScaler scaler;

  @Override
  public void run() throws IOException {
    log.debug("Loading wavelet from file");
    List<Double> waveletList = new ArrayList<Double>();
    try (BufferedReader reader = Files.newBufferedReader(waveletFile, Charset.defaultCharset())) {
      String line;
      while ((line = reader.readLine()) != null) {
        waveletList.add(Double.valueOf(line));
      }
    }

    // Copy to a double[] array to avoid lots of autoboxing
    log.debug("Loaded " + waveletList.size() + " values for wavelet");
    double[] wavelet = new double[waveletList.size()];
    for (int i = 0; i < wavelet.length; i++) {
      wavelet[i] = waveletList.get(i);
    }

    log.debug("Initializing Wavelet scaling interpolator");
    scaler = new ArrayScaler(wavelet);

    // Get the data from the Wig file
    log.debug("Loading Wig data");
    float[] x;
    try (WigFileReader wig = WigFileReader.autodetect(inputFile)) {
      x = wig.query(chr, start, stop).getValues();
    } catch (WigFileException e) {
      log.error("Error retrieving data for interval from Wig file");
      e.printStackTrace();
      throw new CommandLineToolException("Error retrieving data for interval from Wig file");
    }

    // Validate that the parameters are sane
    if (maxLength > x.length) {
      log.error("Maximum wavelet scale size = " + maxLength + ", data length = " + x.length);
      throw new CommandLineToolException("Maximum wavelet scale size > size of data!");
    }

    log.debug("Doing the Wavelet decomposition");
    int numSteps = (maxLength - minLength) / stepSize;
    float[][] matrix = new float[numSteps][x.length];
    for (int i = 0; i < numSteps; i++) {
      // Stretch the wavelet to size l
      int l = minLength + i * stepSize;
      double[] stretchedWavelet = scaler.getScaled(l);
      float sumY = 0, sumSqY = 0;
      for (int j = 0; j < l; j++) {
        sumY += stretchedWavelet[j];
        sumSqY += stretchedWavelet[j] * stretchedWavelet[j];
      }

      for (int j = 0; j < x.length - l; j++) {
        // Calculate the correlation between the signal and the wavelet at this
        // point
        float sumX = 0, sumSqX = 0, sumXY = 0;
        for (int k = 0; k < stretchedWavelet.length; k++) {
          sumX += x[j + k];
          sumSqX += x[j + k] * x[j + k];
          sumXY += x[j + k] * stretchedWavelet[k];
        }
        matrix[i][j + l / 2] = (float) ((l * sumXY - sumX * sumY) / Math.sqrt(l * sumSqX - sumX * sumX) / Math.sqrt(l
            * sumSqY - sumY * sumY));
      }
    }

    // Write to output in matrix2png format
    try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      // Header line
      writer.write(chr);
      for (int bp = start; bp <= stop; bp++) {
        writer.write("\t" + bp);
      }

      for (int i = matrix.length - 1; i >= 0; i--) {
        writer.newLine();
        int l = minLength + i * stepSize;
        writer.write(String.valueOf(l));
        for (int j = 0; j < x.length; j++) {
          writer.write("\t" + matrix[i][j]);
        }
      }
    }
  }

  public static void main(String[] args) {
    new WaveletTransform().instanceMain(args);
  }
}