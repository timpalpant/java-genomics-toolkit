package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class Subtract extends WigMathTool {

	private static final Logger log = Logger.getLogger(Subtract.class);

	@Parameter(names = {"-m", "--minuend"}, description = "Minuend (top - file 1)", required = true)
	public WigFile minuendFile;
	@Parameter(names = {"-s", "--subtrahend"}, description = "Subtrahend (bottom - file 2)", required = true)
	public WigFile subtrahendFile;

	@Override
	public void setup() {
		log.debug("Initializing input files");
		inputs.add(minuendFile);
		inputs.add(subtrahendFile);
		log.debug("Initialized " + inputs.size() + " input files");
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Computing difference for chunk "+chr+":"+start+"-"+stop);
		
		Iterator<WigItem> minuendData = minuendFile.query(chr, start, stop);
		Iterator<WigItem> subtrahendData = subtrahendFile.query(chr, start, stop);
		
		float[] result = WigFile.flattenData(minuendData, start, stop);
		while (subtrahendData.hasNext()) {
			WigItem item = subtrahendData.next();
			for (int i = item.getStartBase(); i <= item.getEndBase(); i++) {
				if (i-start >= 0 && i-start < result.length) {
					result[i-start] -= item.getWigValue();
				}
			}
		}
		
		return result;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new Subtract().instanceMain(args);
	}

}
