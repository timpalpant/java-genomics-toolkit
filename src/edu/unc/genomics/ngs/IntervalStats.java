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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class IntervalStats extends CommandLineTool {

	private static final Logger log = Logger.getLogger(IntervalStats.class);

	@Parameter(description = "Input files", required = true)
	public List<String> inputFiles = new ArrayList<String>();
	@Parameter(names = {"-l", "--loci"}, description = "Loci file (Bed)", required = true)
	public IntervalFile<? extends Interval> lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	
	private List<WigFile> wigs = new ArrayList<>();
	
	@Override
	public void run() throws IOException {		
		log.debug("Initializing input Wig file(s)");
		for (String inputFile : inputFiles) {
			try {
				WigFile wig = WigFile.autodetect(Paths.get(inputFile));
				wigs.add(wig);
			} catch (WigFileException e) {
				log.error("Error initializing Wig input file: " + inputFile);
				e.printStackTrace();
				throw new RuntimeException("Error initializing Wig input file: " + inputFile);
			}
		}
		
		log.debug("Initializing output file");
		int count = 0, skipped = 0;
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			writer.write("#Chr\tStart\tStop\tID\tAlignment\tStrand");
			for (String inputFile : inputFiles) {
				Path p = Paths.get(inputFile);
				writer.write("\t" + p.getFileName().toString());
			}
			writer.newLine();
			
			log.debug("Iterating over all intervals and computing statistics");
			SummaryStatistics stats = new SummaryStatistics();
			for (Interval interval : lociFile) {
				List<Double> means = new ArrayList<>(wigs.size());
				for (WigFile wig : wigs) {
					stats.clear();
					try {
						Iterator<WigItem> result = wig.query(interval);
						while(result.hasNext()) {
							WigItem item = result.next();
							for (int i = item.getStartBase(); i <= item.getEndBase(); i++) {
								stats.addValue(item.getWigValue());
							}
						}
						means.add(stats.getMean());
					} catch (WigFileException e) {
						means.add(Double.NaN);
						skipped++;
					}
				}
				
				writer.write(interval.toBed() + "\t" + StringUtils.join(means, "\t"));
				writer.newLine();
				count++;
			}
		}
		
		lociFile.close();
		for (WigFile wig : wigs) {
			wig.close();
		}
		log.info(count + " intervals processed");
		log.info(skipped + " interval skipped");
	}
	
	public static void main(String[] args) {
		new IntervalStats().instanceMain(args);
	}

}
