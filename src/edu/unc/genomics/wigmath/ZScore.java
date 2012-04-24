package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class ZScore extends WigMathTool {

	private static final Logger log = Logger.getLogger(ZScore.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	
	double mean;
	double stdev;

	@Override
	public void setup() {
		inputs.add(inputFile);
		
		mean = inputFile.mean();
		stdev = inputFile.stdev();
		if(stdev == 0) {
			log.error("Cannot Z-score a file with stdev = 0!");
			throw new CommandLineToolException("Cannot Z-score a file with stdev = 0!");
		}
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		Iterator<WigItem> data = inputFile.query(chr, start, stop);
		float[] result = WigFile.flattenData(data, start, stop);
		
		for (int i = 0; i < result.length; i++) {
			result[i] = (float)((result[i] - mean) / stdev);
		}
		
		return result;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new ZScore().instanceMain(args);
	}

}
