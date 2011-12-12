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

public class Subtract extends WigMathTool {

	private static final Logger log = Logger.getLogger(Subtract.class);

	@Parameter(names = {"-m", "--minuend"}, description = "Minuend (file 1)", required = true)
	public String minuendFile;
	@Parameter(names = {"-s", "--subtrahend"}, description = "Subtrahend (file 2)", required = true)
	public String subtrahendFile;
	
	WigFile minuend;
	WigFile subtrahend;

	@Override
	public void setup() {
		log.debug("Initializing input files");
		
		try {
			minuend = WigFile.autodetect(Paths.get(minuendFile));
			subtrahend = WigFile.autodetect(Paths.get(subtrahendFile));
		} catch (IOException | WigFileException e) {
			log.fatal("Error initializing input Wig files");
			e.printStackTrace();
			System.exit(-1);
		}
		
		inputs.add(minuend);
		inputs.add(subtrahend);
		log.debug("Initialized " + inputs.size() + " input files");
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Computing difference for chunk "+chr+":"+start+"-"+stop);
		
		Iterator<WigItem> minuendData = minuend.query(chr, start, stop);
		Iterator<WigItem> subtrahendData = subtrahend.query(chr, start, stop);
		
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
		Subtract application = new Subtract();
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
