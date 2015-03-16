package edu.unc.genomics.nucleosomes;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Calculate a potential energy landscape from nucleosome occupancy data. See
 * Locke G, et al. (2010) High-throughput sequencing reveals a simple model of
 * nucleosome energetics. PNAS 107: 20998-21003
 * 
 * @author timpalpant
 *
 */
public class PercusDecomposition extends WigMathTool {

  private static final Logger log = Logger.getLogger(PercusDecomposition.class);

  @Parameter(names = { "-d", "--dyads" }, description = "Dyad counts file", required = true, validateWith = ReadablePathValidator.class)
  public Path dyadsFile;
  @Parameter(names = { "-n", "--size" }, description = "Nucleosome size (bp)")
  public int nucleosomeSize = 147;

  private WigFileReader reader;
  int halfNuc = 73;
  float maxOcc = 0;

  @Override
  public void setup() {
    try {
      reader = WigFileReader.autodetect(dyadsFile);
    } catch (IOException e) {
      log.error("IOError opening Wig file");
      e.printStackTrace();
      throw new CommandLineToolException("IOError opening Wig file");
    }
    addInputFile(reader);
    halfNuc = nucleosomeSize / 2;

    log.debug("Initializing statistics");
    DescriptiveStatistics percusStats = new DescriptiveStatistics();
    percusStats.setWindowSize(nucleosomeSize);
    DescriptiveStatistics occupancyStats = new DescriptiveStatistics();
    occupancyStats.setWindowSize(nucleosomeSize);

    log.debug("Computing maximum genome-wide occupancy (normalization factor)");
    for (String chr : reader.chromosomes()) {
      occupancyStats.clear();

      // Walk the chromosome while keeping track of occupancy
      int bp = reader.getChrStart(chr);
      int stop = reader.getChrStop(chr);
      while (bp <= stop) {
        int chunkStart = bp;
        int chunkStop = Math.min(chunkStart + DEFAULT_CHUNK_SIZE - 1, stop);

        try {
          float[] data = reader.query(chr, chunkStart, chunkStop).getValues();
          for (int i = 0; i < data.length; i++) {
            if (Float.isNaN(data[i])) {
              data[i] = 0;
            }

            occupancyStats.addValue(data[i]);
            if (occupancyStats.getSum() > maxOcc) {
              maxOcc = (float) occupancyStats.getSum();
            }
          }
        } catch (WigFileException | IOException e) {
          log.error("Error getting data from input Wig file");
          e.printStackTrace();
          throw new CommandLineToolException("Error getting data from input Wig file");
        }

        bp = chunkStop + 1;
      }
    }
    log.debug("Computed maximum genome-wide occupancy = " + maxOcc);
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    DescriptiveStatistics percusStats = new DescriptiveStatistics();
    percusStats.setWindowSize(nucleosomeSize);
    DescriptiveStatistics occupancyStats = new DescriptiveStatistics();
    occupancyStats.setWindowSize(nucleosomeSize);

    // Pad the query with an additional nucleosome on either end
    int paddedStart = Math.max(chunk.getStart() - nucleosomeSize, reader.getChrStart(chunk.getChr()));
    int paddedStop = Math.min(chunk.getStop() + nucleosomeSize, reader.getChrStop(chunk.getChr()));

    float[] dyads = reader.query(chunk.getChr(), paddedStart, paddedStop).getValues();
    for (int i = 0; i < dyads.length; i++) {
      if (Float.isNaN(dyads[i])) {
        dyads[i] = 0;
      }
    }

    // Calculate normalized occupancy & dyads from the dyads data
    float[] occ = new float[dyads.length];
    for (int i = 0; i < dyads.length; i++) {
      occupancyStats.addValue(dyads[i]);
      if (i - halfNuc >= 0) {
        occ[i - halfNuc] = (float) (occupancyStats.getSum() / maxOcc);
      }
      // Also normalize the dyads data
      dyads[i] /= maxOcc;
    }

    // Prime the summation calculation
    for (int i = halfNuc; i < 3 * halfNuc; i++) {
      double summand = Math.log((1 - occ[i]) / (1 - occ[i] + dyads[i]));
      percusStats.addValue(summand);
    }

    // Assume kb*T = 1 and mu = 0 (can be arbitrarily shifted and scaled)
    // See Eq. S12 in Locke et al. (2010), PNAS
    float[] energies = new float[chunk.length()];
    for (int i = nucleosomeSize; i < dyads.length - nucleosomeSize; i++) {
      double value = Math.log((1 - occ[i] + dyads[i]) / dyads[i]);
      double summand = Math.log((1 - occ[i + halfNuc]) / (1 - occ[i + halfNuc] + dyads[i + halfNuc]));
      percusStats.addValue(summand);
      double summation = percusStats.getSum();
      energies[i - nucleosomeSize] = (float) (value + summation);
    }

    return energies;
  }

  public static void main(String[] args) {
    new PercusDecomposition().instanceMain(args);
  }

}