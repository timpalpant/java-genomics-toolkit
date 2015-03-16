package edu.unc.genomics.nucleosomes;

import java.io.IOException;
import java.util.Iterator;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadMapperTool;
import edu.unc.genomics.io.IntervalFileReader;

/**
 * Count the number of read centers overlapping each base pair in the genome
 * 
 * @author timpalpant
 *
 */
public class MapDyads extends ReadMapperTool {

  @Parameter(names = { "-s", "--size" }, description = "Mononucleosome length (default: read length)")
  public Integer nucleosomeSize;

  @Override
  public float[] compute(IntervalFileReader<? extends Interval> reader, Interval chunk) throws IOException {
    float[] count = new float[chunk.length()];

    // Need to pad the query if artificially shifting read centers
    int paddedStart = chunk.getStart();
    int paddedStop = chunk.getStop();
    if (nucleosomeSize != null && nucleosomeSize > 0) {
      paddedStart = Math.max(chunk.getStart() - nucleosomeSize - 1, 1);
      paddedStop = Math.min(chunk.getStop() + nucleosomeSize + 1, assembly.getChrLength(chunk.getChr()));
    }

    Iterator<? extends Interval> it = reader.query(chunk.getChr(), paddedStart, paddedStop);
    while (it.hasNext()) {
      Interval entry = it.next();
      int center;
      if (nucleosomeSize == null || nucleosomeSize <= 0) {
        center = entry.center();
      } else {
        if (entry.isWatson()) {
          center = entry.getStart() + nucleosomeSize / 2;
        } else {
          center = entry.getStart() - nucleosomeSize / 2;
        }
      }

      // Only map if it is in the current chunk
      if (chunk.getStart() <= center && center <= chunk.getStop()) {
        count[center - chunk.getStart()]++;
      }
    }

    return count;
  }

  public static void main(String[] args) {
    new MapDyads().instanceMain(args);
  }

}