package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.Contig;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileWriter;
import edu.unc.genomics.io.WigFileException;
import edu.ucsc.genome.TrackHeader;

/**
 * For each interval, output a new Wig file of data
 * @author timpalpant
 *
 */
public class SplitWigIntervals extends CommandLineTool {

	private static final Logger log = Logger.getLogger(IntervalStats.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (Wig)", 
             required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Loci file (Bed)", 
             required = true, validateWith = ReadablePathValidator.class)
	public Path lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file name format (%s = each interval ID)")
	public String outputFilePattern = "%s.wig";
	
	@Override
	public void run() throws IOException {
		log.debug("Initializing input file");
		int count = 0, skipped = 0;
		try (WigFileReader wig = WigFileReader.autodetect(inputFile);
         IntervalFileReader<? extends Interval> intervals = IntervalFileReader.autodetect(lociFile)) {
			log.debug("Iterating over all intervals and writing Wig for each");
      TrackHeader header = TrackHeader.newWiggle();
			for (Interval interval : intervals) {
					try {
            Contig query = wig.query(interval);
            header.setName(interval.getId());
            Path outputFile = Paths.get(String.format(outputFilePattern, interval.getId()));
            try (WigFileWriter writer = new WigFileWriter(outputFile, header)) {
              writer.write(query);
            }
					} catch (WigFileException e) {
            log.info("Skipping interval "+interval+" which has no data");
						skipped++;
					}
          count++;
		  }
		}
		
		log.info(count + " intervals processed");
		log.info(skipped + " interval skipped");
	}
	
	public static void main(String[] args) {
		new SplitWigIntervals().instanceMain(args);
	}

}
