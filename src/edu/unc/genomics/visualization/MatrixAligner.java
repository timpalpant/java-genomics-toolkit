package edu.unc.genomics.visualization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.IntervalFileSnifferException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class MatrixAligner {

	private static final Logger log = Logger.getLogger(MatrixAligner.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (Wig)", required = true)
	public String inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Loci file (Bed)", required = true)
	public String lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (matrix2png format)", required = true)
	public String outputFile;
	
	private WigFile wig;
	private List<Interval> intervals;
	
	public void run() throws IOException, WigFileException, IntervalFileSnifferException {
		log.debug("Initializing input Wig file");
		wig = WigFile.autodetect(Paths.get(inputFile));
		
		log.debug("Loading alignment intervals");
		IntervalFile<? extends Interval> loci = IntervalFile.autodetect(Paths.get(lociFile));
		for (Interval interval : loci) {
			intervals.add(interval);
		}
		loci.close();
		
		// Compute the matrix dimensions
		
		
		log.debug("Initializing output file");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
		
		log.debug("Iterating over all intervals");
		int count = 0;
		for (Interval interval : loci) {
			Iterator<WigItem> result = wig.query(interval);
			String[] data = new String[interval.length()];
			Arrays.fill(data, "-");
			while(result.hasNext()) {
				WigItem item = result.next();
				String value = String.valueOf(item.getWigValue());
				for (int i = item.getStartBase(); i <= item.getEndBase(); i++) {
					//data[i-interval.low()] = value;
				}
			}
			
			// Reverse if on the crick strand
			if (interval.isCrick()) {
				ArrayUtils.reverse(data);
			}
			
			// Write to output
		}
		
		wig.close();
		writer.close();
		log.info(count + " intervals processed");
	}
	
	public static void main(String[] args) throws IOException, WigFileException, IntervalFileSnifferException {
		MatrixAligner application = new MatrixAligner();
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
