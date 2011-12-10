package edu.unc.genomics.nucleosomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang3.ArrayUtils;
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

public class AutoCaller {
	
	private static final Logger log = Logger.getLogger(AutoCaller.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public String inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Genomic loci (Bed format)", required = true)
	public String lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public String outputFile;
	@Parameter(names = {"-m", "--min"}, description = "Minimum NRL (bp)")
	public int min = 200;
	@Parameter(names = {"-n", "--max"}, description = "Maximum NRL (bp)")
	public int max = 200;
	
	private WigFile wig;
	
	private int indexOfMax(float[] a) {
		float maxValue = -Float.MAX_VALUE;
		int maxIndex = -1;
		
		for (int i = 0; i < a.length; i++) {
			if (a[i] > max) {
				maxValue = a[i];
				maxIndex = i;
			}
		}
		
		return maxIndex;
	}
	
	public void run() throws IOException, WigFileException {		
		log.debug("Initializing input Wig file");
		wig = WigFile.autodetect(Paths.get(inputFile));
		
		log.debug("Initializing output file");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
		
		log.debug("Initializing loci file");
		IntervalFile<? extends Interval> loci = null;
		try {
			loci = IntervalFile.autodetect(Paths.get(lociFile));
		} catch (IntervalFileSnifferException e) {
			log.fatal("Error autodetecting interval file format");
			e.printStackTrace();
		}

		log.debug("Calling nucleosomes in each window");
		int skipped = 0;
		for (Interval interval : loci) {
			// Find the +1 nucleosome
			Iterator<WigItem> result = wig.query(interval);
			float[] data = WigFile.flattenData(result, interval.getStart(), interval.getStop());
			int plusOne = indexOfMax(Arrays.copyOf(data, min));
		}
		
		loci.close();
		writer.close();
		log.info("Skipped " + skipped + " intervals");
	}
	
	public static void main(String[] args) throws IOException, WigFileException {
		AutoCaller a = new AutoCaller();
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