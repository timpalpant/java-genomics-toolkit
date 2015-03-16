package edu.unc.genomics.converters;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadMapperTool;
import edu.unc.genomics.ValuedInterval;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * Convert interval-based data such as microarray data in Bed, BedGraph, or GFF
 * format to Wig format. Overlapping probes in the original interval dataset are
 * averaged.
 * 
 * @author timpalpant
 *
 */
public class IntervalToWig extends ReadMapperTool {

  private static final Logger log = Logger.getLogger(IntervalToWig.class);

  @Parameter(names = { "-z", "--zero" }, description = "Assume zero where there is no data (default = NaN)")
  public boolean defaultZero = false;

  @Override
  public float[] compute(IntervalFileReader<? extends Interval> reader, Interval chunk) throws IOException {
    float[] sum = new float[chunk.length()];
    int[] count = new int[chunk.length()];

    Iterator<? extends Interval> it = reader.query(chunk);
    while (it.hasNext()) {
      ValuedInterval entry;
      try {
        entry = (ValuedInterval) it.next();
      } catch (ClassCastException e) {
        log.error("Input file does not appear to be a valued interval format (Bed/BedGraph/GFF/GeneTrack)!");
        throw new CommandLineToolException(
            "Input file does not appear to be a valued interval format (Bed/BedGraph/GFF/GeneTrack)!");
      }

      if (entry.getValue() != null) {
        int entryStart = Math.max(chunk.getStart(), entry.low());
        int entryStop = Math.min(chunk.getStop(), entry.high());
        for (int i = entryStart; i <= entryStop; i++) {
          sum[i - chunk.getStart()] += entry.getValue().floatValue();
          count[i - chunk.getStart()]++;
        }
      }
    }

    // Calculate the average at each base pair in the chunk
    for (int i = 0; i < sum.length; i++) {
      if (count[i] != 0 || !defaultZero) {
        sum[i] /= count[i];
      }
    }

    return sum;
  }

  public static void main(String[] args) {
    new IntervalToWig().instanceMain(args);
  }

}
