package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class FilterOutlierRegions extends WigMathTool {

	private static final Logger log = Logger.getLogger(FilterOutlierRegions.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-w", "--window"}, description = "Window size", required = true)
	public int windowSize;
	@Parameter(names = {"-t", "--threshold"}, description = "Threshold (x mean)")
	public float threshold = 3;
	
	double mean;
	DescriptiveStatistics stats;

	@Override
	public void setup() {
		inputs.add(inputFile);
		
		mean = inputFile.mean();
		
		stats = new DescriptiveStatistics();
		stats.setWindowSize(windowSize);
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		stats.clear();
		// Pad the query with an additional half window on either end
		int paddedStart = Math.max(start-windowSize/2, inputFile.getChrStart(chr));
		int paddedStop = Math.min(stop+windowSize/2, inputFile.getChrStop(chr));
			
		Iterator<WigItem> data = inputFile.query(chr, paddedStart, paddedStop);
		float[] result = WigFile.flattenData(data, paddedStart, paddedStop);
		
		// Prime the moving window statistics
		for (int i = 0; i < windowSize; i++) {
			stats.addValue(result[i]);
		}
		
		return result;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new FilterOutlierRegions().instanceMain(args);
	}

}
