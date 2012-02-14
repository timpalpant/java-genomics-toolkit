package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.PositiveIntegerValidator;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class MovingAverageSmooth extends WigMathTool {

	private static final Logger log = Logger.getLogger(MovingAverageSmooth.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-w", "--width"}, description = "Width of kernel (bp)")
	public int width = 10;
	
	WigFile input;
	DescriptiveStatistics stats;

	@Override
	public void setup() {
		inputs.add(inputFile);
		
		log.debug("Initializing statistics");
		stats = new DescriptiveStatistics();
		stats.setWindowSize(width);
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Smoothing chunk "+chr+":"+start+"-"+stop);
		// Pad the query so that we can provide values for the ends
		int queryStart = Math.max(start-width/2, input.getChrStart(chr));
		int queryStop = Math.min(stop+width/2, input.getChrStop(chr));
		Iterator<WigItem> result = input.query(chr, queryStart, queryStop);
		float[] data = WigFile.flattenData(result, queryStart, queryStop);
		
		float[] smoothed = new float[stop-start+1];
		for (int bp = start; bp <= stop; bp++) {
			stats.addValue(data[bp-queryStart]);
			if (bp-start-width/2 >= 0) {
				smoothed[bp-start-width/2] = (float) stats.getMean();
			}
		}
		
		return smoothed;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new MovingAverageSmooth().instanceMain(args);
	}

}
