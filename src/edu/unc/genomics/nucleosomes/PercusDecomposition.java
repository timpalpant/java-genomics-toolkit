package edu.unc.genomics.nucleosomes;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.wigmath.WigMathTool;

public class PercusDecomposition extends WigMathTool {
	
	private static final Logger log = Logger.getLogger(PercusDecomposition.class);

	@Parameter(names = {"-d", "--dyads"}, description = "Dyad counts file", required = true, validateWith = ReadablePathValidator.class)
	public WigFile dyadsFile;
	@Parameter(names = {"-n", "--size"}, description = "Nucleosome size (bp)")
	public int nucleosomeSize = 147;
	
	int halfNuc = 73;
	DescriptiveStatistics percusStats;
	DescriptiveStatistics occupancyStats;
	double maxOcc = 0;
	
	@Override
	public void setup() {
		addInputFile(dyadsFile);
		halfNuc = nucleosomeSize / 2;
		
		log.debug("Initializing statistics");
		percusStats = new DescriptiveStatistics();
		percusStats.setWindowSize(nucleosomeSize);
		occupancyStats = new DescriptiveStatistics();
		occupancyStats.setWindowSize(nucleosomeSize);
		
		log.debug("Computing maximum genome-wide occupancy (normalization factor)");
		for (String chr : dyadsFile.chromosomes()) {
			occupancyStats.clear();
			
			// Walk the chromosome while keeping track of occupancy
			int bp = dyadsFile.getChrStart(chr);
			int stop = dyadsFile.getChrStop(chr);
			while (bp <= stop) {
				int chunkStart = bp;
				int chunkStop = Math.min(chunkStart+DEFAULT_CHUNK_SIZE-1, stop);
				
				try {
					Iterator<WigItem> result = dyadsFile.query(chr, chunkStart, chunkStop);
					float[] data = WigFile.flattenData(result, chunkStart, chunkStop, 0);
					for (int i = 0; i < data.length; i++) {
						occupancyStats.addValue(data[i]);
						if (occupancyStats.getSum() > maxOcc) {
							maxOcc = occupancyStats.getSum();
						}
					}
				} catch (WigFileException | IOException e) {
					log.error("Error getting data from input Wig file");
					e.printStackTrace();
					throw new CommandLineToolException("Error getting data from input Wig file");
				}
				
				bp = chunkStop+1;
			}
		}
		log.debug("Computed maximum genome-wide occupancy = " + maxOcc);
	}

	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		// Reset sliding window stats
		percusStats.clear();
		occupancyStats.clear();
		
		// Pad the query with an additional nucleosome on either end
		int paddedStart = Math.max(start-nucleosomeSize, getMaxChrStart(inputs, chr));
		int paddedStop = Math.min(stop+nucleosomeSize, getMinChrStop(inputs, chr));
		
		Iterator<WigItem> result = dyadsFile.query(chr, paddedStart, paddedStop);
		float[] dyads = WigFile.flattenData(result, start-nucleosomeSize, stop+nucleosomeSize, 0);
		
		// Calculate normalized occupancy & dyads from the dyads data
		float[] occ = new float[dyads.length];
		for (int i = 0; i < dyads.length; i++) {
			occupancyStats.addValue(dyads[i]);
			if (i-halfNuc >= 0) {
				occ[i-halfNuc] = (float) (occupancyStats.getSum() / maxOcc);
			}
			// Also normalize the dyads data
			dyads[i] /= maxOcc;
		}
		
		// Prime the summation calculation
		for (int i = halfNuc; i < 3*halfNuc; i++) {
			double summand = Math.log((1-occ[i])/(1-occ[i]+dyads[i]));
			percusStats.addValue(summand);
		}
		
		// Assume kb*T = 1 and mu = 0 (can be arbitrarily shifted and scaled)
		// See Eq. S12 in Locke et al. (2010), PNAS
		float[] energies = new float[stop-start+1];
		for (int i = nucleosomeSize; i < dyads.length-nucleosomeSize; i++) {
			double value = Math.log((1-occ[i]+dyads[i])/dyads[i]);
			double summand = Math.log((1-occ[i+halfNuc])/(1-occ[i+halfNuc]+dyads[i+halfNuc]));
			percusStats.addValue(summand);
			double summation = percusStats.getSum();
			energies[i-nucleosomeSize] = (float) (value+summation);
		}
		
		return energies;
	}
	
	public static void main(String[] args) {
		new PercusDecomposition().instanceMain(args);
	}

}