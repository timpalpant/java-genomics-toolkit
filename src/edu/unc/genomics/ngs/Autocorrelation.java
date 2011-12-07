package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.IntervalFileSnifferException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.util.ArrayUtils;

public class Autocorrelation {
	
	private static final Logger log = Logger.getLogger(Autocorrelation.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public String inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Genomic loci (Bed format)", required = true)
	public String lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public String outputFile;
	@Parameter(names = {"-m", "--max"}, description = "Autocorrelation limit (bp)")
	public int limit = 200;
	
	private WigFile wig;
	
	private void abs2(float[] data) {
		for (int i = 0; i < data.length; i+=2) {
			data[i] = data[i]*data[i] + data[i+1]*data[i+1];
			data[i+1] = 0;
		}
	}
	
	public void run() throws IOException, WigFileException {		
		log.debug("Initializing input Wig file");
		wig = WigFile.autodetect(Paths.get(inputFile));
		
		log.debug("Initializing output file");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
		//writer.write("ORF\tChromosome\tStart (+1 Nuc)\tStop (3' Nuc)\tL\tAutocorrelation\n");
		
		log.debug("Initializing loci file");
		IntervalFile<? extends Interval> loci = null;
		try {
			loci = IntervalFile.autodetect(Paths.get(lociFile));
		} catch (IntervalFileSnifferException e) {
			log.fatal("Error autodetecting interval file format");
			e.printStackTrace();
		}

		log.debug("Computing autocorrelation for each window");
		int skipped = 0;
		for (Interval interval : loci) {
			if (interval.length() < limit) {
				log.debug("Skipping interval: " + interval.toString());
				skipped++;
				continue;
			}
			
			Iterator<WigItem> wigIter;
			try {
				wigIter = wig.query(interval);
			} catch (IOException | WigFileException e) {
				log.debug("Skipping interval: " + interval.toString());
				skipped++;
				continue;
			}
			
			float[] data = WigFile.flattenData(wigIter, interval.getStart(), interval.getStop());
			
			// Compute the autocorrelation with the Wiener-Khinchin theorem
			FloatFFT_1D fft = new FloatFFT_1D(data.length);
			fft.realForward(data);
			abs2(data);
			fft.realInverse(data, true);

			writer.write(StringUtils.join(data, "\t"));
			writer.newLine();
		}
		
		loci.close();
		writer.close();
		log.info("Skipped " + skipped + " intervals");
	}
	
	public static void main(String[] args) throws IOException, WigFileException {
		Autocorrelation a = new Autocorrelation();
		JCommander jc = new JCommander(a);
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			jc.usage();
			System.exit(-1);
		}
		
		a.run();
	}
}