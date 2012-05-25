package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Smooth a (Big)Wig file with a moving average filter
 * @author timpalpant
 *
 */
public class MovingAverageSmooth extends WigMathTool {

	private static final Logger log = Logger.getLogger(MovingAverageSmooth.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-w", "--width"}, description = "Width of kernel (bp)")
	public int width = 10;
	
	WigFileReader reader;
	DescriptiveStatistics stats;

	@Override
	public void setup() {
		try {
			reader = WigFileReader.autodetect(inputFile);
		} catch (IOException e) {
			log.error("IOError opening Wig file");
			e.printStackTrace();
			throw new CommandLineToolException(e.getMessage());
		}
		inputs.add(reader);
		
		log.debug("Initializing statistics");
		stats = new DescriptiveStatistics();
		stats.setWindowSize(width);
	}
	
	@Override
	public float[] compute(Interval chunk) throws IOException, WigFileException {
		// Pad the query so that we can provide values for the ends
		int queryStart = Math.max(chunk.getStart()-width/2, reader.getChrStart(chunk.getChr()));
		int queryStop = Math.min(chunk.getStart()+width/2, reader.getChrStop(chunk.getChr()));
		float[] data = reader.query(chunk.getChr(), queryStart, queryStop).getValues();
		
		float[] smoothed = new float[chunk.length()];
		for (int bp = chunk.getStart(); bp <= chunk.getStop(); bp++) {
			stats.addValue(data[bp-queryStart]);
			if (bp-chunk.getStart()-width/2 >= 0) {
				smoothed[bp-chunk.getStart()-width/2] = (float) stats.getMean();
			}
		}
		
		return smoothed;
	}
	
	public static void main(String[] args) throws IOException, WigFileException {
		new MovingAverageSmooth().instanceMain(args);
	}

}
