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
 * Shift a Wig file to have a specified mean
 * @author timpalpant
 *
 */
public class Shift extends WigMathTool {

	private static final Logger log = Logger.getLogger(Shift.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-m", "--mean"}, description = "New mean")
	public double newMean = 0;
	
	WigFileReader reader;
	double shift;

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
		
		shift = newMean - reader.mean();
		log.info("Shifting to mean: "+newMean);
	}
	
	@Override
	public float[] compute(Interval chunk) throws IOException, WigFileException {
		float[] result = reader.query(chunk).getValues();
		for (int i = 0; i < result.length; i++) {
			result[i] += shift;
		}
		
		return result;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new Shift().instanceMain(args);
	}

}
