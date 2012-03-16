package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class Scale extends WigMathTool {

	private static final Logger log = Logger.getLogger(Scale.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-m", "--multiplier"}, description = "Multiplier (scale factor, default = 1/mean)")
	public Double multiplier;
	

	@Override
	public void setup() {
		inputs.add(inputFile);
		
		if (multiplier == null || multiplier == 0) {
			multiplier = inputFile.numBases() / inputFile.total();
			log.info("Scaling to mean: "+inputFile.mean());
		}
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Computing difference for chunk "+chr+":"+start+"-"+stop);
		
		Iterator<WigItem> data = inputFile.query(chr, start, stop);
		float[] result = WigFile.flattenData(data, start, stop);
		
		for (int i = 0; i < result.length; i++) {
			result[i] = (float) (multiplier * result[i]);
		}
		
		return result;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new Scale().instanceMain(args);
	}

}
