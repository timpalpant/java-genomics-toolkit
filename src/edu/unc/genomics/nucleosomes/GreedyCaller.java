package edu.unc.genomics.nucleosomes;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.NucleosomeCall;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileWriter;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.SortUtils;

/**
 * Calls stereotypic nucleosome positions from MNase-seq data using a greedy
 * algorithm
 * 
 * @author timpalpant
 *
 */
public class GreedyCaller extends CommandLineTool {

  private static final Logger log = Logger.getLogger(GreedyCaller.class);

  @Parameter(names = { "-d", "--dyads" }, description = "Dyad counts file", required = true, validateWith = ReadablePathValidator.class)
  public Path dyadsFile;
  @Parameter(names = { "-s", "--smoothed" }, description = "Smoothed dyad counts file", required = true, validateWith = ReadablePathValidator.class)
  public Path smoothedDyadsFile;
  @Parameter(names = { "-n", "--size" }, description = "Nucleosome size (bp)")
  public int nucleosomeSize = 147;
  @Parameter(names = { "-o", "--output" }, description = "Output file", required = true)
  public Path outputFile;

  public void run() throws IOException {
    int halfNuc = nucleosomeSize / 2;
    int count = 0;
    try (WigFileReader dyadsReader = WigFileReader.autodetect(dyadsFile);
        WigFileReader smoothedDyadsReader = WigFileReader.autodetect(smoothedDyadsFile);
        IntervalFileWriter<NucleosomeCall> writer = new IntervalFileWriter<>(outputFile)) {
      // Write header
      writer
          .writeComment("#chr\tstart\tstop\tlength\tlengthStdev\tdyad\tdyadStdev\tconditionalPosition\tdyadMean\toccupancy");

      for (String chr : smoothedDyadsReader.chromosomes()) {
        log.debug("Processing chromosome " + chr);
        int chunkStart = smoothedDyadsReader.getChrStart(chr);
        int chrStop = smoothedDyadsReader.getChrStop(chr);
        while (chunkStart < chrStop) {
          int chunkStop = chunkStart + DEFAULT_CHUNK_SIZE - 1;
          int paddedStart = Math.max(chunkStart - nucleosomeSize, smoothedDyadsReader.getChrStart(chr));
          int paddedStop = Math.min(chunkStop + nucleosomeSize, smoothedDyadsReader.getChrStop(chr));
          log.debug("Processing chunk " + chunkStart + "-" + chunkStop);

          float[] dyads;
          float[] smoothed;
          try {
            dyads = dyadsReader.query(chr, paddedStart, paddedStop).getValues();
            smoothed = smoothedDyadsReader.query(chr, paddedStart, paddedStop).getValues();
          } catch (IOException | WigFileException e) {
            throw new CommandLineToolException(e);
          }

          int[] sortedIndices = SortUtils.sortIndices(smoothed);

          // Proceed through the data in descending order
          for (int j = sortedIndices.length - 1; j >= 0; j--) {
            int i = sortedIndices[j];
            int dyad = paddedStart + i;

            if (smoothed[i] > 0) {
              int nucStart = Math.max(paddedStart, dyad - halfNuc);
              int nucStop = Math.min(dyad + halfNuc, paddedStop);
              NucleosomeCall call = new NucleosomeCall(chr, nucStart, nucStop);
              call.setDyad(dyad);

              // Find the dyad mean
              double occupancy = 0;
              double weightedSum = 0;
              double smoothedSum = 0;
              for (int bp = nucStart; bp <= nucStop; bp++) {
                occupancy += dyads[bp - paddedStart];
                weightedSum += dyads[bp - paddedStart] * bp;
                smoothedSum += smoothed[bp - paddedStart];
              }
              call.setOccupancy(occupancy);
              double dyadMean = weightedSum / occupancy;

              if (occupancy > 0) {
                call.setDyadMean((int) Math.round(dyadMean));
                call.setConditionalPosition(smoothed[i] / smoothedSum);

                // Find the variance
                double sumOfSquares = 0;
                for (int bp = nucStart; bp <= nucStop; bp++) {
                  sumOfSquares += dyads[bp - paddedStart] * Math.pow(bp - dyadMean, 2);
                }
                double variance = sumOfSquares / occupancy;
                call.setDyadStdev(Math.sqrt(variance));

                // variance = mean of squares minus square of mean
                // this is more efficient but causing cancellation with floats
                // double variance = sumOfSquares/occupancy -
                // Math.pow(weightedSum/occupancy, 2);

                // Only write nucleosomes within the current chunk to disk
                if (chunkStart <= dyad && dyad <= chunkStop) {
                  writer.write(call);
                  count++;
                }

                // Don't allow nucleosome calls overlapping this nucleosome
                int low = Math.max(i - nucleosomeSize, 0);
                int high = Math.min(i + nucleosomeSize, smoothed.length - 1);
                for (int k = low; k <= high; k++) {
                  smoothed[k] = 0;
                }
              }
            }
          }

          chunkStart = chunkStop + 1;
        }
      }
    }

    log.info("Called " + count + " nucleosomes");
  }

  public static void main(String[] args) {
    new GreedyCaller().instanceMain(args);
  }
}