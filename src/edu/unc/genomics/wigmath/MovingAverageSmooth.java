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
  int nterms, nends, nfull, multiplier;

  @Override
  public void setup() {
    try {
      reader = WigFileReader.autodetect(inputFile);
    } catch (IOException e) {
      throw new CommandLineToolException(e);
    }
    inputs.add(reader);
    
    nterms = width + step - 1;
    multiplier = Math.min(width, step);
    nends = multiplier - 1;
    nfull = nterms - 2*nends;
  }

  @Override
  public float[] compute(Interval chunk) throws IOException, WigFileException {
    // Pad the query so that we can provide values for the ends
    int queryStart = Math.max(chunk.getStart() - width / 2, reader.getChrStart(chunk.getChr()));
    int queryStop = Math.min(chunk.getStop() + width / 2, reader.getChrStop(chunk.getChr()));
    Contig contig = reader.query(chunk.getChr(), queryStart, queryStop);
    // We can simply express the effect of a span > 1 on the result
    // of moving average, so use this optimization now.
    int nValues = (int) Math.ceil(((float) chunk.length()) / step);
    float[] result = new float[nValues];
    for (int i = 0; i < result.length; i++) {
      float x = 0;
      int bp = contig.getStart() + i*step - width/2;
      
      for (int j = 1; j <= nends; j++) {
        x += j * contig.get(bp);
        bp += 1;
      }
      
      for (int j = 1; j <= nfull; j++) {
        x += multiplier * contig.get(bp);
        bp += 1;
      }
      
      for (int j = 0; j < nends; j++) {
        x += (nends-j) * contig.get(bp);
      }
      
      result[i] = x / (step*width);
    }
    return result;
  }

  public static void main(String[] args) throws IOException, WigFileException {
    new MovingAverageSmooth().instanceMain(args);
  }

}
