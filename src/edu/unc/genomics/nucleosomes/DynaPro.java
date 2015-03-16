package edu.unc.genomics.nucleosomes;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Contig;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.io.WigFileWriter;

/**
 * Solve single-particle hard-rod statistical equilibria with the DynaPro
 * algorithm See Morozov AV, et al. (2009) Using DNA mechanics to predict in
 * vitro nucleosome positions and formation energies. Nucleic Acids Res 37:
 * 4707-4722
 * 
 * @author timpalpant
 *
 */
public class DynaPro extends CommandLineTool {

  private static final Logger log = Logger.getLogger(DynaPro.class);

  @Parameter(names = { "-i", "--input" }, description = "Energy landscape", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-n", "--size" }, description = "Nucleosome size (bp)")
  public int nucleosomeSize = 147;
  @Parameter(names = { "-m", "--mean" }, description = "Shift energy landscape to have mean")
  public Double newMean;
  @Parameter(names = { "-v", "--variance" }, description = "Scale energy landscape to variance")
  public Double newVar;
  @Parameter(names = { "-o", "--output" }, description = "Output file (Wig)", required = true)
  public Path outputFile;

  Float shift, scale;

  @Override
  public void run() throws IOException {
    TrackHeader header = TrackHeader.newWiggle();
    try (WigFileReader reader = WigFileReader.autodetect(inputFile);
        WigFileWriter writer = new WigFileWriter(outputFile, header)) {
      if (newMean != null) {
        log.debug("Shifting mean of energy landscape from " + reader.mean() + " to " + newMean);
        shift = (float) (newMean - reader.mean());
      }

      if (newVar != null) {
        log.debug("Rescaling variance of energy landscape from " + Math.pow(reader.stdev(), 2) + " to " + newVar);
        scale = (float) Math.sqrt(newVar / Math.pow(reader.stdev(), 2));
      }

      for (String chr : reader.chromosomes()) {
        log.debug("Processing chromosome " + chr);
        int start = reader.getChrStart(chr);
        int stop = reader.getChrStop(chr);

        // Process the chromosome
        float[] energy = reader.query(chr, start, stop).getValues();

        // Assume 0 if data is missing
        for (int i = 0; i < energy.length; i++) {
          if (Float.isNaN(energy[i])) {
            energy[i] = 0;
          }
        }

        // Shift and rescale the energy landscape if specified
        if (shift != null) {
          for (int i = 0; i < energy.length; i++) {
            energy[i] += shift;
          }
        }
        if (scale != null) {
          for (int i = 0; i < energy.length; i++) {
            energy[i] *= scale;
          }
        }

        // Compute the probabilities
        float[] forward = new float[energy.length];
        for (int i = nucleosomeSize; i < energy.length; i++) {
          double factor = 1 + Math.exp(forward[i - nucleosomeSize] - forward[i - 1] - energy[i - nucleosomeSize]);
          forward[i] = (float) (forward[i - 1] + Math.log(factor));
        }

        float[] backward = new float[energy.length];
        for (int i = energy.length - nucleosomeSize - 1; i > 0; i--) {
          double factor = 1 + Math.exp(backward[i + nucleosomeSize] - backward[i + 1] - energy[i - 1]);
          backward[i] = (float) (backward[i + 1] + Math.log(factor));
        }

        float[] p = new float[energy.length];
        for (int i = 0; i < energy.length - nucleosomeSize; i++) {
          p[i] = (float) Math.exp(forward[i] - energy[i] + backward[i + nucleosomeSize] - backward[1]);
        }

        // Write the chromosome to output
        writer.write(new Contig(chr, start, stop, p));
      }
    } catch (WigFileException e) {
      log.error("Error getting data from Wig file");
      e.printStackTrace();
      throw new CommandLineToolException("Error getting data from Wig file");
    }
  }

  public static void main(String[] args) throws IOException, WigFileException {
    new DynaPro().instanceMain(args);
  }

}
