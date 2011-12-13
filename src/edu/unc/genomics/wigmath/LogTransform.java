package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class LogTransform extends WigMathTool {

	private static final Logger log = Logger.getLogger(LogTransform.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-b", "--base"}, description = "Logarithm base (default = 2)")
	public double base = 2;
	
	private double baseChange;

	@Override
	public void setup() {
		baseChange = Math.log(base);
		inputs.add(inputFile);
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Computing difference for chunk "+chr+":"+start+"-"+stop);
		
		Iterator<WigItem> data = inputFile.query(chr, start, stop);
		float[] result = WigFile.flattenData(data, start, stop);
		
		for (int i = 0; i < result.length; i++) {
			result[i] = (float) (Math.log(result[i]) / baseChange);
		}
		
		return result;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new LogTransform().instanceMain(args);
	}

}
