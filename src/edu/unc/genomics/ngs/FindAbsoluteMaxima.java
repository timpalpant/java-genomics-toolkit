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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.IntervalFileSnifferException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class FindAbsoluteMaxima {

	private static final Logger log = Logger.getLogger(FindAbsoluteMaxima.class);

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
		for (Interval interval : loci) {
			writer.write(interval.toBed());
			for (WigFile wig : wigs) {
				float maxValue = -Float.MAX_VALUE;
				int maxima = -1;
				Iterator<WigItem> results = wig.query(interval);
				while (results.hasNext()) {
					WigItem item = results.next();
					if (item.getWigValue() > maxValue) {
						maxValue = item.getWigValue();
						maxima = (item.getStartBase() + item.getEndBase()) / 2;
					}
				}
				writer.write("\t" + maxima);
			}
			writer.newLine();
			count++;
		}
		
		writer.close();
		loci.close();
		for (WigFile wig : wigs) {
			wig.close();
		}
		log.info(count + " intervals processed");
	}
	
	public static void main(String[] args) throws IOException, WigFileException, IntervalFileSnifferException {
		FindAbsoluteMaxima application = new FindAbsoluteMaxima();
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
