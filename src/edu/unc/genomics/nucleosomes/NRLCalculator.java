package edu.unc.genomics.nucleosomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
	
	@Override
	public void run() throws IOException {		
		log.debug("Initializing input file");
		NucleosomeCallsFile nucsFile = new NucleosomeCallsFile(inputFile);
		
		log.debug("Initializing output file");
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			log.debug("Calculating nucleosome spacing for each interval");
			NucleosomeCall.DyadComparator comparator = new NucleosomeCall.DyadComparator();
			for (Interval interval : lociFile) {
				writer.write(interval.toBed());
				
				// Get all of the nucleosomes within this interval
				Iterator<NucleosomeCall> it = nucsFile.query(interval);
				List<NucleosomeCall> intervalNucs = new ArrayList<NucleosomeCall>();
				while (it.hasNext()) {
					intervalNucs.add(it.next());
				}
	
				if (intervalNucs.size() > 1) {
					// Sort the list by nucleosome position
					Collections.sort(intervalNucs, comparator);
					if (interval.isCrick()) {
						Collections.reverse(intervalNucs);
					}
					
					for (int i = 1; i < intervalNucs.size(); i++) {
						writer.write("\t" + Math.abs(intervalNucs.get(i).getDyad()-intervalNucs.get(i-1).getDyad()));
					}
				}
				
				writer.newLine();
			}
		}
		
		lociFile.close();
		nucsFile.close();
	}
	
	public static void main(String[] args) throws IOException {
		new NRLCalculator().instanceMain(args);
	}
}