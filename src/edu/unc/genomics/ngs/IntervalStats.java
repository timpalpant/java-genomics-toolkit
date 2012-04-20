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

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.WigStatistic;

public class IntervalStats extends CommandLineTool {

	private static final Logger log = Logger.getLogger(IntervalStats.class);

	@Parameter(description = "Input files", required = true)
	public List<String> inputFiles = new ArrayList<String>();
	@Parameter(names = {"-l", "--loci"}, description = "Loci file (Bed)", required = true)
	public IntervalFile<? extends Interval> lociFile;
	@Parameter(names = {"-s", "--stat"}, description = "Statistic to compute (mean/min/max)")
	public String stat = "mean";
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	
	private List<WigFile> wigs = new ArrayList<>();
	
	@Override
	public void run() throws IOException {
		WigStatistic s = WigStatistic.fromName(stat);
		if (s == null) {
			log.error("Unknown statistic: "+stat);
			throw new CommandLineToolException("Unknown statistic: "+stat+". Options are mean, min, max");
		} else {
			log.debug("Using statistic: "+stat);
		}
		
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
			
			log.debug("Iterating over all intervals and computing "+stat);
			for (Interval interval : lociFile) {
				writer.write(interval.toBed());
				
				for (WigFile wig : wigs) {
					try {
						Iterator<WigItem> result = wig.query(interval);
						float value = Float.NaN;
						switch (s) {
						case MEAN:
							value = WigFile.mean(result, interval.getStart(), interval.getStop());
							break;
						case MIN:
							value = WigFile.min(result, interval.getStart(), interval.getStop());
							break;
						case MAX:
							value = WigFile.max(result, interval.getStart(), interval.getStop());
							break;
						}
						writer.write("\t" + value);
					} catch (WigFileException e) {
						writer.write("\t" + Float.NaN);
						skipped++;
					}
				}
				
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
