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

public class ZScorer extends WigMathProgram {

	private static final Logger log = Logger.getLogger(ZScorer.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public String inputFile;
	
	WigFile input;
	double mean;
	double stdev;

	@Override
	public void setup() {
		log.debug("Initializing input file");
		try {
			input = WigFile.autodetect(Paths.get(inputFile));
		} catch (IOException | WigFileException e) {
			log.fatal("Error initializing input Wig files");
			e.printStackTrace();
			System.exit(-1);
		}
		inputs.add(input);
		
		mean = input.mean();
		stdev = input.stdev();
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Computing difference for chunk "+chr+":"+start+"-"+stop);
		Iterator<WigItem> data = input.query(chr, start, stop);
		float[] result = WigFile.flattenData(data, start, stop);
		
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
		ZScorer application = new ZScorer();
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
