package edu.unc.genomics.converters;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.GeneTrackEntry;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadMapperTool;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * Convert a GeneTrack format file to Wig, optionally shifting and merging the
 * +/- strands
 * 
 * @author timpalpant
 *
 */
public class GeneTrackToWig extends ReadMapperTool {

  private static final Logger log = Logger.getLogger(GeneTrackToWig.class);

  @Parameter(names = { "-z", "--zero" }, description = "Assume zero where there is no data (default = NaN)")
  public boolean defaultZero = false;
  @Parameter(names = { "-s", "--shift" }, description = "Shift from 5' end (bp)")
  public Integer shift;

  @Override
  public float[] compute(IntervalFileReader<? extends Interval> reader, Interval chunk) throws IOException {
    float[] sum = new float[chunk.length()];
    int[] count = new int[chunk.length()];

    // Pad to shift length
    int paddedStart = chunk.getStart();
    int paddedStop = chunk.getStop();
    if (shift != null) {
      paddedStart = Math.max(chunk.getStart() - shift - 1, 1);
      paddedStop = Math.min(chunk.getStop() + shift + 1, assembly.getChrLength(chunk.getChr()));
    }

    Iterator<? extends Interval> it = reader.query(chunk.getChr(), paddedStart, paddedStop);
    while (it.hasNext()) {
      GeneTrackEntry entry;
      try {
        entry = (GeneTrackEntry) it.next();
      } catch (ClassCastException e) {
        log.error("Input file does not appear to be GeneTrack format!");
        throw new CommandLineToolException("Input file does not appear to be GeneTrack format!");
      }

      int entryPos = entry.getStart();
      if (shift == null || shift == 0) {
        if (entryPos >= chunk.getStart() && entryPos <= chunk.getStop()) {
          sum[entryPos - chunk.getStart()] += entry.getValue().floatValue();
          count[entryPos - chunk.getStart()]++;
        }
      } else {
        if (entry.getForward() > 0) {
          int forwardShift = entryPos + shift;
          if (forwardShift >= chunk.getStart() && forwardShift <= chunk.getStop()) {
            sum[forwardShift - chunk.getStart()] += entry.getForward();
            count[forwardShift - chunk.getStart()]++;
          }
        }

        if (entry.getReverse() > 0) {
          int reverseShift = entryPos - shift;
          if (reverseShift >= chunk.getStart() && reverseShift <= chunk.getStop()) {
            sum[reverseShift - chunk.getStart()] += entry.getReverse();
            count[reverseShift - chunk.getStart()]++;
          }
        }
      }
    }

    // Put NaNs where there was no data
    for (int i = 0; i < sum.length; i++) {
      if (count[i] == 0 && !defaultZero) {
        sum[i] = Float.NaN;
      }
    }

    return sum;
  }

  public static void main(String[] args) {
    new GeneTrackToWig().instanceMain(args);
  }

}
