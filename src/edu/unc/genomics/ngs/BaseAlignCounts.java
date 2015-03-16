package edu.unc.genomics.ngs;

import java.io.IOException;
import java.util.Iterator;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadMapperTool;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * This tool calculates the coverage of sequencing reads (or any interval data)
 * and creates a new Wig file with the number of reads overlapping each base
 * pair.
 * 
 * @author timpalpant
 *
 */
public class BaseAlignCounts extends ReadMapperTool {

  @Parameter(names = { "-x", "--extend" }, description = "Extend reads from 5' end (default = fragment length)")
  public Integer extend = -1;

  @Override
  public float[] compute(IntervalFileReader<? extends Interval> reader, Interval chunk) throws IOException {
    float[] count = new float[chunk.length()];

    // Need to pad the query if extending reads
    int paddedStart = chunk.getStart();
    int paddedStop = chunk.getStop();
    if (extend != null && extend != -1) {
      paddedStart = Math.max(chunk.getStart() - extend - 1, 1);
      paddedStop = Math.min(chunk.getStop() + extend + 1, assembly.getChrLength(chunk.getChr()));
    }

    Iterator<? extends Interval> it = reader.query(chunk.getChr(), paddedStart, paddedStop);
    while (it.hasNext()) {
      Interval entry = it.next();
      int entryStop = entry.getStop();
      if (extend != null && extend != -1) {
        if (entry.isWatson()) {
          entryStop = entry.getStart() + extend - 1;
        } else {
          entryStop = entry.getStart() - extend + 1;
        }
      }

      // Clamp to the current chunk
      int low = Math.max(Math.min(entry.getStart(), entryStop), chunk.getStart());
      int high = Math.min(Math.max(entry.getStart(), entryStop), chunk.getStop());
      for (int i = low; i <= high; i++) {
        count[i - chunk.getStart()]++;
      }
    }

    return count;
  }

  public static void main(String[] args) {
    new BaseAlignCounts().instanceMain(args);
  }
}