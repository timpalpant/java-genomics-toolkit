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
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class FindAbsoluteMaxima extends CommandLineTool {

	private static final Logger log = Logger.getLogger(FindAbsoluteMaxima.class);

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
			writer.write("#Chr\tStart\tStop\tID\tValue\tStrand");
			for (String inputFile : inputFiles) {
				Path p = Paths.get(inputFile);
				writer.write("\t" + p.getFileName().toString());
			}
			writer.newLine();
			
			log.debug("Iterating over all intervals and finding maxima");
			for (Interval interval : lociFile) {
				writer.write(interval.toBed());
				for (WigFile wig : wigs) {
					float maxValue = -Float.MAX_VALUE;
					int maxima = -1;
					try {
						Iterator<WigItem> results = wig.query(interval);
						while (results.hasNext()) {
							WigItem item = results.next();
							if (item.getWigValue() > maxValue) {
								maxValue = item.getWigValue();
								maxima = (item.getStartBase() + item.getEndBase()) / 2;
							}
						}
						writer.write("\t" + maxima);
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
		new FindAbsoluteMaxima().instanceMain(args);
	}

}
