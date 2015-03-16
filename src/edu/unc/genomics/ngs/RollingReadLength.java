package edu.unc.genomics.ngs;

import java.io.IOException;
import java.util.Iterator;

import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadMapperTool;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * Creates a new Wig file with the mean read length of reads covering each base
 * pair.
 * 
 * @author timpalpant
 *
 */
public class RollingReadLength extends ReadMapperTool {

  @Override
  public float[] compute(IntervalFileReader<? extends Interval> reader, Interval chunk) throws IOException {
    int[] sum = new int[chunk.length()];
    int[] count = new int[chunk.length()];

    Iterator<? extends Interval> it = reader.query(chunk);
    while (it.hasNext()) {
      Interval entry = it.next();
      int entryStart = Math.max(entry.low(), chunk.getStart());
      int entryStop = Math.min(entry.high(), chunk.getStop());
      for (int i = entryStart; i <= entryStop; i++) {
        sum[i - chunk.getStart()] += entry.length();
        count[i - chunk.getStart()]++;
      }
    }

    // Calculate the average at each base pair
    float[] avg = new float[chunk.length()];
    for (int i = 0; i < avg.length; i++) {
      if (count[i] == 0) {
        avg[i] = Float.NaN;
      } else {
        avg[i] = ((float) sum[i]) / count[i];
      }
    }

    return avg;
  }

  public static void main(String[] args) {
    new RollingReadLength().instanceMain(args);
  }
}