package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class GaussianSmooth extends WigMathTool {

	private static final Logger log = Logger.getLogger(GaussianSmooth.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public String inputFile;
	@Parameter(names = {"-s", "--stdev"}, description = "Standard deviation of Gaussian (bp)")
	public double stdev = 2;
	
	WigFile input;
	float[] filter;

	@Override
	public void setup() {
		log.debug("Initializing input file");
		try {
			input = WigFile.autodetect(Paths.get(inputFile));
		} catch (IOException | WigFileException e) {
			log.fatal("Error initializing input Wig files");
			e.printStackTrace();
			System.exit(-1);
		}
		inputs.add(input);
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
		
		Iterator<WigItem> result = input.query(chr, start, stop);
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
		GaussianSmooth application = new GaussianSmooth();
		JCommander jc = new JCommander(application);
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			jc.usage();
			System.exit(-1);
		}
		
		application.run();
	}

}
