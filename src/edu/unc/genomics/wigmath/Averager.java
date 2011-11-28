package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class Averager extends WigMathProgram {

	private static final Logger log = Logger.getLogger(Averager.class);

	@Parameter(description = "Input files", required = true)
	public List<String> inputFiles = new ArrayList<String>();
	
	int numFiles;

	@Override
	public void setup() {
		if (inputFiles.size() < 2) {
			log.info("No reason to average < 2 files. Exiting");
			System.exit(1);
		}
		
		log.debug("Initializing input files");
		for (String inputFile : inputFiles) {
			try {
				addInputFile(WigFile.autodetect(Paths.get(inputFile)));
			} catch (IOException | WigFileException e) {
				log.error("Error initializing input Wig file: " + inputFile);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		log.debug("Initialized " + inputs.size() + " input files");
		
		numFiles = inputs.size();
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Computing average for chunk "+chr+":"+start+"-"+stop);
		
		int length = stop - start + 1;
		float[] avg = new float[length];
		
		for (WigFile wig : inputs) {
			Iterator<WigItem> data = wig.query(chr, start, stop);
			while (data.hasNext()) {
				WigItem item = data.next();
				for (int i = item.getStartBase(); i <= item.getEndBase(); i++) {
					if (i-start >= 0 && i-start < avg.length) {
						avg[i-start] += item.getWigValue();
					}
				}
			}
		}
		
		for (int i = 0; i < avg.length; i++) {
			avg[i] = avg[i] / numFiles;
		}
		
		return avg;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		Averager application = new Averager();
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
