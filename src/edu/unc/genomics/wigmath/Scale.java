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
 * Scale a Wig file by a constant, or normalize to its mean value
 * @author timpalpant
 *
 */
public class Scale extends WigMathTool {

	private static final Logger log = Logger.getLogger(Scale.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-m", "--multiplier"}, description = "Multiplier (scale factor, default = 1/mean)")
	public Double multiplier;
	
	WigFileReader reader;
	
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
		
		if (multiplier == null || multiplier == 0) {
			multiplier = reader.numBases() / reader.total();
			log.info("Scaling to mean: "+reader.mean());
		}
	}
	
	@Override
	public float[] compute(Interval chunk) throws IOException, WigFileException {
		float[] result = reader.query(chunk).getValues();
		for (int i = 0; i < result.length; i++) {
			result[i] = (float) (multiplier * result[i]);
		}
		
		return result;
	}
	
	public static void main(String[] args) throws IOException, WigFileException {
		new Scale().instanceMain(args);
	}

}
