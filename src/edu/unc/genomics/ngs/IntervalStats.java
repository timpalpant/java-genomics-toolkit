package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import edu.unc.genomics.util.ArrayUtils;

public class IntervalStats {

	private static final Logger log = Logger.getLogger(IntervalStats.class);

	@Parameter(description = "Input files", required = true)
	public List<String> inputFiles = new ArrayList<String>();
	@Parameter(names = {"-l", "--loci"}, description = "Loci file (Bed)", required = true)
	public String lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public String outputFile;
	
	public void run() throws IOException, WigFileException, IntervalFileSnifferException {		
		log.debug("Initializing input Wig file(s)");
		List<WigFile> wigs = new ArrayList<WigFile>();
		for (String inputFile : inputFiles) {
			WigFile wig = WigFile.autodetect(Paths.get(inputFile));
			wigs.add(wig);
		}
		
		log.debug("Initializing output file");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
		writer.write("#Chr\tStart\tStop\tID\tValue\tStrand");
		for (String inputFile : inputFiles) {
			Path p = Paths.get(inputFile);
			writer.write("\t" + p.getFileName().toString());
		}
		writer.newLine();
		
		log.debug("Initializing interval file");
		IntervalFile<? extends Interval> loci = IntervalFile.autodetect(Paths.get(lociFile));
		
		log.debug("Iterating over all intervals and computing statistics");
		int count = 0;
		SummaryStatistics stats = new SummaryStatistics();
		for (Interval interval : loci) {
			log.debug("Processing interval: " + interval.toString());
			List<Double> means = new ArrayList<Double>();
			for (WigFile wig : wigs) {
				stats.clear();
				try {
					log.debug("...querying " + wig.getPath().getFileName());
					Iterator<WigItem> result = wig.query(interval);
					while(result.hasNext()) {
						WigItem item = result.next();
						for (int i = item.getStartBase(); i <= item.getEndBase(); i++) {
							stats.addValue(item.getWigValue());
						}
					}
					means.add(stats.getMean());
				} catch (WigFileException e) {
					log.debug("Skipping: " + interval.toString());
					means.add(Double.NaN);
				}
			}
			
			writer.write(interval.toBed() + "\t" + ArrayUtils.join(means, "\t"));
			writer.newLine();
			count++;
		}
		
		writer.close();
		log.info(count + " intervals processed");
	}
	
	public static void main(String[] args) throws IOException, WigFileException, IntervalFileSnifferException {
		IntervalStats application = new IntervalStats();
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
