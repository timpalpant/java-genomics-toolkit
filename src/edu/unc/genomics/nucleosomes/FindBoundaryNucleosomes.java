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

public class FindBoundaryNucleosomes {
	
	private static final Logger log = Logger.getLogger(FindBoundaryNucleosomes.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (nucleosome calls)", required = true)
	public String inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Boundary loci (Bed format)", required = true)
	public String lociFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
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
		NucleosomeCall.DyadComparator comparator = new NucleosomeCall.DyadComparator();
		int skipped = 0;
		for (Interval interval : loci) {
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
		
		loci.close();
		writer.close();
		
		log.info("Skipped "+skipped+" intervals with 0 nucleosomes");
	}
	
	public static void main(String[] args) throws IOException {
		FindBoundaryNucleosomes a = new FindBoundaryNucleosomes();
		JCommander jc = new JCommander(a);
		jc.setProgramName(FindBoundaryNucleosomes.class.getSimpleName());
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			jc.usage();
			System.exit(-1);
		}
		
		a.run();
	}
}