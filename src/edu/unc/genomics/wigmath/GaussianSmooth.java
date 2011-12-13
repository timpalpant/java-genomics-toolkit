package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;
import edu.unc.genomics.PositiveIntegerValidator;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class GaussianSmooth extends WigMathTool {

	private static final Logger log = Logger.getLogger(GaussianSmooth.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-s", "--stdev"}, description = "Standard deviation of Gaussian (bp)", validateWith = PositiveIntegerValidator.class)
	public int stdev = 2;
	
	float[] filter;

	@Override
	public void setup() {
		inputs.add(inputFile);
	}
	
	private void initializeFilter(int length) {
		filter = new float[length];
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Smoothing chunk "+chr+":"+start+"-"+stop);
		
		// Recompute the filter if the window size has changed
		int length = stop - start + 1;
		if (filter == null || filter.length != length) {
			initializeFilter(length);
		}
		
		Iterator<WigItem> result = inputFile.query(chr, start, stop);
		float[] data = WigFile.flattenData(result, start, stop);
		
		// Convolution is multiplication in frequency space
		FloatFFT_1D fft = new FloatFFT_1D(data.length);
		fft.realForward(data);
		for (int i = 0; i < data.length; i++) {
			data[i] = filter[i] * data[i];
		}
		fft.realInverse(data, false);
		
		return data;
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
