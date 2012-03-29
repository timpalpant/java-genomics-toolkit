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

public class FindBoundaryNucleosomes extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(FindBoundaryNucleosomes.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (nucleosome calls)", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Boundary loci (Bed format)", required = true)
	public IntervalFile<? extends Interval> lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	
	private Map<String,List<NucleosomeCall>> nucs = new HashMap<>();
	
	private List<NucleosomeCall> getIntervalNucleosomes(Interval i) {
		List<NucleosomeCall> intervalNucs = new ArrayList<>();
		if (nucs.containsKey(i.getChr())) {
			for (NucleosomeCall call : nucs.get(i.getChr())) {
				if (call.getDyad() >= i.low() && call.getDyad() <= i.high()) {
					intervalNucs.add(call);
				}
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
		int skipped = 0;
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			log.debug("Finding boundary nucleosomes for each interval");
			// Write header
			writer.write("#chr\tlow\thigh\tid\talignment\tstrand\t5' Dyad Position\t3' Dyad Position");
			writer.newLine();
			NucleosomeCall.DyadComparator comparator = new NucleosomeCall.DyadComparator();
			for (Interval interval : lociFile) {
				writer.write(interval.toBed());
				
				// Get all of the nucleosomes within this interval
				List<NucleosomeCall> intervalNucs = getIntervalNucleosomes(interval);
	
				if (intervalNucs.size() > 0) {
					// Sort the list by nucleosome position
					Collections.sort(intervalNucs, comparator);
					if (interval.isCrick()) {
						Collections.reverse(intervalNucs);
					}
					
					int fivePrime = intervalNucs.get(0).getDyad();
					int threePrime = intervalNucs.get(intervalNucs.size()-1).getDyad();
					writer.write("\t"+fivePrime+"\t"+threePrime);
				} else {
					skipped++;
					writer.write("\tNA\tNA");
				}
				
				writer.newLine();
			}
		}
		
		lociFile.close();
		log.info("Skipped "+skipped+" intervals with 0 nucleosomes");
	}
	
	public static void main(String[] args) {
		new FindBoundaryNucleosomes().instanceMain(args);
	}
}