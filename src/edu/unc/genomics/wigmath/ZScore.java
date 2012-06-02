package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;

/**
 * Z-score the values in a Wig file: calculate normal scores = (value-mean)/stdev
 * @author timpalpant
 *
 */
public class ZScore extends WigMathTool {

	private static final Logger log = Logger.getLogger(ZScore.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	
	WigFileReader reader;
	float mean, stdev;

	@Override
	public void setup() {
		try {
			reader = WigFileReader.autodetect(inputFile);
		} catch (IOException e) {
			throw new CommandLineToolException(e);
		}
		inputs.add(reader);
		
		mean = (float) reader.mean();
		stdev = (float) reader.stdev();
		if(stdev == 0) {
			throw new CommandLineToolException("Cannot Z-score a file with stdev = 0!");
		}
		
		log.info("Mean = "+mean);
		log.info("StDev = "+stdev);
	}
	
	@Override
	public float[] compute(Interval chunk) throws IOException, WigFileException {
		float[] result = reader.query(chunk).getValues();
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
