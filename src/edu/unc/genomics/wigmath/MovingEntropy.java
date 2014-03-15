package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Smooth a (Big)Wig file with a moving average filter
 * @author timpalpant
 *
 */
public class MovingEntropy extends WigMathTool {

	@Parameter(names = {"-i", "--input"}, description = "Input file", 
	    required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-w", "--width"}, description = "Width of window (bp)")
	public int width = 147;
	@Parameter(names = {"-b", "--base"}, description = "Base of logarithm")
	public float base = 2;
	
	WigFileReader reader;
  double logBase;

	@Override
	public void setup() {
		try {
			reader = WigFileReader.autodetect(inputFile);
		} catch (IOException e) {
			throw new CommandLineToolException(e);
		}
		inputs.add(reader);
    logBase = Math.log(base);
	}
	
	@Override
	public float[] compute(Interval chunk) throws IOException, WigFileException {
		// Pad the query so that we can provide values for the ends
		int queryStart = Math.max(chunk.getStart()-width/2, reader.getChrStart(chunk.getChr()));
		int queryStop = Math.min(chunk.getStop()+width/2, reader.getChrStop(chunk.getChr()));
		float[] data = reader.query(chunk.getChr(), queryStart, queryStop).getValues();
    
		// Scale the signal by the maximum sum in a window so that it represents
		// the probability that a nucleosome occupies that base pair in that window
		// You should probably remove outliers (esp. CNVs) first
    float[] entropy = new float[chunk.length()];
		DescriptiveStatistics stats = new DescriptiveStatistics();
		stats.setWindowSize(width);
		for (int bp = chunk.getStart(); bp <= chunk.getStop(); bp++) {
			stats.addValue(data[bp-queryStart]);
			if (bp-chunk.getStart()-width/2 >= 0) {
        double h = 0;
        double windowSum = stats.getSum();
        for (double value : stats.getValues()) {
          double p = value / windowSum;
          h += p * Math.log(p) / logBase;
        }
				entropy[bp-chunk.getStart()-width/2] = (float) -h;
			}
		}
		
		return entropy;
	}
	
	public static void main(String[] args) throws IOException, WigFileException {
		new MovingEntropy().instanceMain(args);
	}

}
