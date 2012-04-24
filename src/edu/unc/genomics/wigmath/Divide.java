package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class Divide extends WigMathTool {

	private static final Logger log = Logger.getLogger(Divide.class);

	@Parameter(names = {"-n", "--numerator"}, description = "Dividend / Numerator (file 1)", required = true)
	public WigFile dividendFile;
	@Parameter(names = {"-d", "--denominator"}, description = "Divisor / Denominator (file 2)", required = true)
	public WigFile divisorFile;

	@Override
	public void setup() {		
		inputs.add(dividendFile);
		inputs.add(divisorFile);
		log.debug("Initialized " + inputs.size() + " input files");
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		Iterator<WigItem> dividendData = dividendFile.query(chr, start, stop);
		Iterator<WigItem> divisorData = divisorFile.query(chr, start, stop);
		
		float[] result = WigFile.flattenData(dividendData, start, stop);
		while (divisorData.hasNext()) {
			WigItem item = divisorData.next();
			for (int i = item.getStartBase(); i <= item.getEndBase(); i++) {
				if (i-start >= 0 && i-start < result.length) {
					if (item.getWigValue() != 0) {
						result[i-start] /= item.getWigValue();
					} else {
						result[i-start] = Float.NaN;
					}
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
		new Divide().instanceMain(args);
	}

}
