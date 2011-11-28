package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class MovingAverageSmooth extends WigMathProgram {

	private static final Logger log = Logger.getLogger(MovingAverageSmooth.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public String inputFile;
	@Parameter(names = {"-w", "--width"}, description = "Width of kernel (bp)")
	public int width = 10;
	
	WigFile input;
	DescriptiveStatistics stats;

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
		
		log.debug("Initializing statistics");
		stats = new DescriptiveStatistics();
		stats.setWindowSize(width);
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		log.debug("Smoothing chunk "+chr+":"+start+"-"+stop);
		// Pad the query so that we can provide values for the ends
		int queryStart = Math.max(start-width/2, input.getChrStart(chr));
		int queryStop = Math.min(stop+width/2, input.getChrStop(chr));
		Iterator<WigItem> result = input.query(chr, queryStart, queryStop);
		float[] data = WigFile.flattenData(result, queryStart, queryStop);
		
		float[] smoothed = new float[stop-start+1];
		for (int bp = start; bp <= stop; bp++) {
			stats.addValue(data[bp-queryStart]);
			if (bp-start-width/2 >= 0) {
				smoothed[bp-start-width/2] = (float) stats.getMean();
			}
		}
		
		return data;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		MovingAverageSmooth application = new MovingAverageSmooth();
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
