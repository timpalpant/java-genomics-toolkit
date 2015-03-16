package edu.unc.genomics.nucleosomes;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

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
 * Attempt to predict dinucleosome signal from mononucleosome data using a
 * simple probabilistic model
 * 
 * @author timpalpant
 *
 */
public class PredictDinucleosomes extends WigMathTool {

  private static final Logger log = Logger.getLogger(PredictDinucleosomes.class);

  @Parameter(names = { "-i", "--input" }, description = "Dyad density (Wig)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-d", "--distribution" }, description = "Dinucleosome read length distribution", required = true, validateWith = ReadablePathValidator.class)
  public Path distributionFile;
  @Parameter(names = { "-n", "--nsize" }, description = "Assumed nucleosome size (bp)")
  public int nucleosomeSize = 147;

  WigFileReader reader;
  float[] distribution = new float[100];
  int minL = Integer.MAX_VALUE, maxL = 0;
  float max;

  @Override
  public void setup() {
    try {
      reader = WigFileReader.autodetect(inputFile);
    } catch (IOException e) {
      throw new CommandLineToolException(e);
    }
    addInputFile(reader);

    log.debug("Loading fragment length distribution");
    float total = 0;
    try (BufferedReader reader = Files.newBufferedReader(distributionFile, Charset.defaultCharset())) {
      String line;
      while ((line = reader.readLine()) != null) {
        // Parse the line
        String[] entry = line.split("\t");
        if (entry.length != 2) {
          throw new CommandLineToolException("Invalid format for fragment length distribution file");
        }
        int length = Integer.parseInt(entry[0]);
        float percent = Float.parseFloat(entry[1]);
        // Expand the sonication distribution array if necessary
        if (length >= distribution.length) {
          distribution = Arrays.copyOf(distribution, Math.max(distribution.length + 100, length + 1));
        }
        if (length < minL) {
          minL = length;
        }
        if (length > maxL) {
          maxL = length;
        }
        distribution[length] = percent;
        total += percent;
      }
    } catch (IOException e) {
      log.fatal("Error loading fragment length distribution");
      e.printStackTrace();
      throw new CommandLineToolException("Error loading fragment length distribution");
    }
    // Truncate the array to the minimum possible size
    distribution = Arrays.copyOfRange(distribution, 0, maxL + 1);
    log.debug("Loaded fragment distribution for lengths: " + minL + "-" + maxL + "bp");

    // Normalize the sonication distribution so that it has total 1
    for (int i = 0; i < distribution.length; i++) {
      distribution[i] /= total;
    }

    max = (float) reader.max();
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    int paddedStart = Math.max(chunk.getStart() - maxL, reader.getChrStart(chunk.getChr()));
    int paddedStop = Math.min(chunk.getStop() + maxL, reader.getChrStop(chunk.getChr()));

    Contig data = reader.query(chunk.getChr(), paddedStart, paddedStop);
    float[] result = data.get(chunk.getStart() - maxL, chunk.getStop() + maxL);

    for (int i = 0; i < result.length; i++) {
      result[i] /= max;
    }

    float[] prediction = new float[chunk.length()];
    for (int l = minL; l <= maxL; l++) {
      int internucleosomeDistance = l - nucleosomeSize;
      if (internucleosomeDistance <= 0) {
        continue;
      }

      for (int i = 0; i < result.length - internucleosomeDistance; i++) {
        // Probability of having a dinucleosome with mononucleosome centers at
        // (i) and (i+l)
        float p = distribution[l] * result[i] * result[i + internucleosomeDistance];
        for (int j = i - maxL - nucleosomeSize / 2; j <= i - maxL + nucleosomeSize / 2; j++) {
          if (j >= 0 && j < prediction.length) {
            prediction[j] += p;
          }
        }
      }
    }

    return prediction;
  }

  /**
   * @param args
   * @throws WigFileException
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, WigFileException {
    new PredictDinucleosomes().instanceMain(args);
  }

}
