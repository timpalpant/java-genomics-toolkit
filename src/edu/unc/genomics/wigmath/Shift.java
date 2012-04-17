package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class Shift extends WigMathTool {

	private static final Logger log = Logger.getLogger(Shift.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-m", "--mean"}, description = "New mean")
	public double newMean = 0;
	
	private double shift;

	@Override
	public void setup() {
		inputs.add(inputFile);
		shift = newMean - inputFile.mean();
		log.info("Shifting to mean: "+newMean);
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		Iterator<WigItem> data = inputFile.query(chr, start, stop);
		float[] result = WigFile.flattenData(data, start, stop);
		
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
