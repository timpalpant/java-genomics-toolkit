package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class DivideWig extends WigMathProgram {

	private static final Logger log = Logger.getLogger(DivideWig.class);

	@Parameter(names = {"-n", "--numerator"}, description = "Dividend / Numerator (file 1)", required = true)
	public String dividendFile;
	@Parameter(names = {"-d", "--denominator"}, description = "Divisor / Denominator (file 2)", required = true)
	public String divisorFile;
	
	WigFile dividend;
	WigFile divisor;

	@Override
	public void setup() {
		log.debug("Initializing input files");
		
		try {
			dividend = WigFile.autodetect(Paths.get(dividendFile));
			divisor = WigFile.autodetect(Paths.get(divisorFile));
		} catch (IOException | WigFileException e) {
			log.fatal("Error initializing input Wig files");
			e.printStackTrace();
			System.exit(-1);
		}
		
		inputs.add(dividend);
		inputs.add(divisor);
		log.debug("Initialized " + inputs.size() + " input files");
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Computing difference for chunk "+chr+":"+start+"-"+stop);
		
		Iterator<WigItem> dividendData = dividend.query(chr, start, stop);
		Iterator<WigItem> divisorData = divisor.query(chr, start, stop);
		
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
		DivideWig application = new DivideWig();
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
