package edu.unc.genomics.nucleosomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFile;

public class NRLCalculator extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(NRLCalculator.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (nucleosome calls)", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Genomic loci (Bed format)", required = true)
	public IntervalFile<? extends Interval> lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (NRL for each gene)", required = true)
	public Path outputFile;
	
	private Map<String,List<NucleosomeCall>> nucs = new HashMap<>();
	
	private List<NucleosomeCall> getIntervalNucleosomes(Interval i) {
		List<NucleosomeCall> intervalNucs = new ArrayList<>();
		for (NucleosomeCall call : nucs.get(i.getChr())) {
			if (call.getDyad() >= i.low() && call.getDyad() <= i.high()) {
				intervalNucs.add(call);
			}
		}
		
		return intervalNucs;
	}
	
	@Override
	public void run() throws IOException {		
		log.debug("Initializing input file");
		NucleosomeCallsFile nucsFile = new NucleosomeCallsFile(inputFile);
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
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			log.debug("Calculating nucleosome spacing for each interval");
			NucleosomeCall.DyadComparator comparator = new NucleosomeCall.DyadComparator();
			for (Interval interval : lociFile) {
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
		}
		
		lociFile.close();
	}
	
	public static void main(String[] args) throws IOException {
		new NRLCalculator().instanceMain(args);
	}
}