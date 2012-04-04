package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class GaussianSmooth extends WigMathTool {

	private static final Logger log = Logger.getLogger(GaussianSmooth.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-s", "--stdev"}, description = "Standard deviation of Gaussian (bp)")
	public int stdev = 20;
	
	float[] filter;

	@Override
	public void setup() {
		inputs.add(inputFile);
		
		// Use a window size equal to +/- 3 SD's
		filter = new float[6*stdev+1];
		float sum = 0;
		for (int i = 0; i < filter.length; i++) {
			float x = i - 3*stdev;
			float value = (float) Math.exp(-(x*x) / (2*stdev*stdev));
			filter[i] = value;
			sum += value;
		}
		for (int i = 0; i < filter.length; i++) {
			filter[i] /= sum;
		}
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Smoothing chunk "+chr+":"+start+"-"+stop);
		
		// Pad the query for smoothing
		int paddedStart = Math.max(start-3*stdev, inputFile.getChrStart(chr));
		int paddedStop = Math.min(stop+3*stdev, inputFile.getChrStop(chr));
		Iterator<WigItem> result = inputFile.query(chr, paddedStart, paddedStop);
		float[] data = WigFile.flattenData(result, start-3*stdev, stop+3*stdev, 0);
		
		// Convolve the data with the filter
		float[] smoothed = new float[stop-start+1];
		for (int i = 0; i < smoothed.length; i++) {
			for (int j = 0; j < filter.length; j++) {
				smoothed[i] += data[i+j] * filter[j];
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
		new GaussianSmooth().instanceMain(args);
	}

}
