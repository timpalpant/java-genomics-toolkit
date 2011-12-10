package edu.unc.genomics.nucleosomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.IntervalFileSnifferException;

public class NRLCalculator {
	
	private static final Logger log = Logger.getLogger(NRLCalculator.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (nucleosome calls)", required = true)
	public String inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Genomic loci (Bed format)", required = true)
	public String lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (NRL for each gene)", required = true)
	public String outputFile;
	
	private Map<String,List<NucleosomeCall>> nucs = new HashMap<String,List<NucleosomeCall>>();
	
	private List<NucleosomeCall> getIntervalNucleosomes(Interval i) {
		List<NucleosomeCall> intervalNucs = new ArrayList<NucleosomeCall>();
		for (NucleosomeCall call : nucs.get(i.getChr())) {
			if (call.getDyad() >= i.low() && call.getDyad() <= i.high()) {
				intervalNucs.add(call);
			}
		}
		
		return intervalNucs;
	}
	
	public void run() throws IOException {		
		log.debug("Initializing input file");
		NucleosomeCallsFile nucsFile = new NucleosomeCallsFile(Paths.get(inputFile));
		log.debug("Loading all nucleosomes");
		for (NucleosomeCall nuc : nucsFile) {
			if (nuc == null) continue;
			if (!nucs.containsKey(nuc.getChr())) {
				nucs.put(nuc.getChr(), new ArrayList<NucleosomeCall>());
			}
			nucs.get(nuc.getChr()).add(nuc);
		}
		nucsFile.close();
		
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

		log.debug("Calculating nucleosome spacing for each interval");
		NucleosomeDyadComparator comparator = new NucleosomeDyadComparator();
		for (Interval interval : loci) {
			writer.write(interval.toBed());
			
			// Get all of the nucleosomes within this interval
			List<NucleosomeCall> intervalNucs = getIntervalNucleosomes(interval);

			if (intervalNucs.size() > 1) {
				// Sort the list by nucleosome position
				Collections.sort(intervalNucs, comparator);
				if (interval.isCrick()) {
					Collections.reverse(intervalNucs);
				}
				
				for (int i = 1; i < Math.min(intervalNucs.size(), 10); i++) {
					writer.write("\t" + Math.abs(intervalNucs.get(i).getDyad()-intervalNucs.get(i-1).getDyad()));
				}
			}
			
			writer.newLine();
		}
		
		loci.close();
		writer.close();
	}
	
	private class NucleosomeDyadComparator implements Comparator<NucleosomeCall> {

		@Override
		public int compare(NucleosomeCall o1, NucleosomeCall o2) {
			if (o1.getDyad() == o2.getDyad()) {
				return 0;
			} else if (o1.getDyad() < o2.getDyad()) {
				return -1;
			} else {
				return 1;
			}
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		NRLCalculator a = new NRLCalculator();
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