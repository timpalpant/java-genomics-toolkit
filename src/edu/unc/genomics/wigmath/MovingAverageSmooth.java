package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.Contig;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

import edu.unc.utils.FFTUtils;

/**
 * Smooth a (Big)Wig file with a moving average filter
 * 
 * @author timpalpant
 *
 */
public class MovingAverageSmooth extends WigMathTool {

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-w", "--width" }, description = "Width of kernel (bp)")
  public int width = 11;

  WigFileReader reader;

  @Override
  public void setup() {
    try {
      reader = WigFileReader.autodetect(inputFile);
    } catch (IOException e) {
      throw new CommandLineToolException(e);
    }
    inputs.add(reader);
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    // Pad the query so that we can provide values for the ends
    int queryStart = Math.max(chunk.getStart() - width / 2, reader.getChrStart(chunk.getChr()));
    int queryStop = Math.min(chunk.getStop() + width / 2, reader.getChrStop(chunk.getChr()));
    Contig contig = reader.query(chunk.getChr(), queryStart, queryStop);
    int nValues = (int) Math.ceil(((float) chunk.length()) / step);
    float[] result = new float[nValues];
    for (int i = 0; i < result.length; i++) {
      float x = 0;
      int start, stop, n;
      if (step < width) {
        n = width;
        start = contig.getStart() + i*step + step/2 - width/2;
        stop = contig.getStart() + i*step + n;
      } else {
        n = step;
        start = contig.getStart() + i*step;
        stop = contig.getStart() + n;
      }
      
      for (int bp = start; bp <= stop; bp++) {
        x += contig.get(bp);
      }
      result[i] = x / n;
    }
    return result;
  }

  public static void main(String[] args) throws IOException, WigFileException {
    new MovingAverageSmooth().instanceMain(args);
  }

}
