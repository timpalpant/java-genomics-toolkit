package edu.unc.genomics.nucleosomes;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.InclusionExclusion;

/**
 * Attempt to predict FAIRE signal from nucleosome occupancy data using a simple probabilistic model
 * @author timpalpant
 *
 */
public class PredictFAIRESignal extends WigMathTool {

	private static final Logger log = Logger.getLogger(PredictFAIRESignal.class);

	@Parameter(names = {"-i", "--input"}, description = "Nucleosome occupancy (Wig)", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-s", "--sonication"}, description = "Sonication distribution", required = true, validateWith = ReadablePathValidator.class)
	public Path sonicationFile;
	@Parameter(names = {"-c", "--crosslinking"}, description = "FAIRE efficiency / crosslinking coefficient")
	public float crosslink = 1;
	@Parameter(names = {"-x", "--extend"}, description = "In silico read extension (bp), -1 for paired-end")
	public int extend = 250;

	WigFileReader reader;
	volatile float[] sonication = new float[100];
	int minL = Integer.MAX_VALUE, maxL = 0;
	float maxOcc;
	
	@Override
	public void setup() {
		try {
			reader = WigFileReader.autodetect(inputFile);
		} catch (IOException e) {
			throw new CommandLineToolException(e);
		}
		addInputFile(reader);
		
		log.debug("Loading sonication fragment length distribution");
		float total = 0;
		try(BufferedReader reader = Files.newBufferedReader(sonicationFile, Charset.defaultCharset())) {
			String line;
			while ((line = reader.readLine()) != null) {
				// Parse the line
				String[] entry = line.split("\t");
				if (entry.length != 2) {
					throw new CommandLineToolException("Invalid format for sonication distribution file");
				}
				int length = Integer.parseInt(entry[0]);
				float percent = Float.parseFloat(entry[1]);
				// Expand the sonication distribution array if necessary
				if (length >= sonication.length) {
					sonication = Arrays.copyOf(sonication, Math.max(sonication.length+100, length+1));
				}
				if (length < minL) {
					minL = length;
				}
				if (length > maxL) {
					maxL = length;
				}
				sonication[length] = percent;
				total += percent;
			}
		} catch (IOException e) {
			log.fatal("Error loading sonication fragment length distribution");
			e.printStackTrace();
			throw new CommandLineToolException("Error loading sonication fragment length distribution");
		}
		// Truncate the array to the minimum possible size
		sonication = Arrays.copyOfRange(sonication, 0, maxL);
		log.debug("Loaded sonication distribution for lengths: "+minL+"-"+maxL+"bp");
		
		// Normalize the sonication distribution so that it has total 1
		for (int i = 0; i < sonication.length; i++) {
			sonication[i] /= total;
		}
		
		// Store the maximum occupancy
		maxOcc = (float) reader.max();
	}
	
	@Override
	public float[] compute(Interval chunk) throws IOException, WigFileException {
		int paddedStart = Math.max(chunk.getStart()-maxL, reader.getChrStart(chunk.getChr()));
		int paddedStop = Math.min(chunk.getStop()+maxL, reader.getChrStop(chunk.getChr()));
		
		Contig data = reader.query(chunk.getChr(), paddedStart, paddedStop);
		float[] result = data.get(chunk.getStart()-maxL, chunk.getStop()+maxL);
		
		// Scale the occupancy by the maximum occupancy so that it represents
		// the probability that a base pair is occupied by a nucleosome
		for (int i = 0; i < result.length; i++) {
			result[i] /= maxOcc;
		}
		
		float[] prediction;
		if (extend < 0) {
			prediction = paired(chunk, result);
		} else {
			prediction = single(chunk, result);
		}
		
		return prediction;
	}
	
	/**
	 * Single-end prediction, with artificial uniform extension
	 * @param chunk
	 * @param occ
	 * @return
	 */
	private float[] single(Interval chunk, float[] occ) {
		float[] watson = new float[occ.length];
		float[] crick = new float[occ.length];
		// Consider all possible fragment lengths
		for (int i = minL; i < sonication.length; i++) {
			// No need to count if there are no fragments of this length
			if (sonication[i] == 0) {
				continue;
			}
			
			// Starting at each base pair in the chunk
			for (int j = 0; j < occ.length-i; j++) {
				// Calculate the probability that this fragment is occupied by a nucleosome
				// using the inclusion-exclusion principle with nucleosome width = 147bp;
				float pOccupied = InclusionExclusion.independent(occ, j, j+i);
				// Calculate the probability that this fragment survives FAIRE
				float pFAIRE = 1 - crosslink*pOccupied;
				// And weight the probability by the sonication distribution
				float pFragment = sonication[i]*pFAIRE;
				
				// Add its probability at the +/- ends, weighted by fragment abundance
				watson[j] += pFragment;
				crick[j+i-1] += pFragment;
			}
		}
		
		float[] prediction = new float[chunk.length()];
		for (int i = 0; i < occ.length; i++) {
			for (int j = 0; j < extend; j++) {
				// Extend on the + strand
				if (i+j-maxL > 0 && i+j-maxL < prediction.length) {
					prediction[i+j-maxL] += watson[i];
				}
				
				// Extend on the - strand
				if (i-j-maxL > 0 && i-j-maxL < prediction.length) {
					prediction[i-j-maxL] += crick[i];
				}
			}
		}
		
		return prediction;
	}
	
	/**
	 * Paired-end, extend to the actual length of each fragment
	 * @return
	 */
	private float[] paired(Interval chunk, float[] occ) {
		float[] prediction = new float[chunk.length()];
		// Consider all possible fragment lengths
		for (int i = minL; i < sonication.length; i++) {
			// No need to count if there are no fragments of this length
			if (sonication[i] == 0) {
				continue;
			}
			
			// Starting at each base pair in the chunk
			for (int j = 0; j < occ.length-i; j++) {
				// Calculate the probability that this fragment is occupied by a nucleosome
				// using the inclusion-exclusion principle with nucleosome width = 147bp;
				float pOccupied = InclusionExclusion.independent(occ, j, j+i);
				// Calculate the probability that this fragment survives FAIRE
				float pFAIRE = 1 - crosslink*pOccupied;
				// And weight the probability by the sonication distribution
				float pFragment = sonication[i]*pFAIRE;
				
				// Add its probability to the occupancy
				for (int k = 0; k < i; k++) {
					if (j+k-maxL > 0 && j+k-maxL < prediction.length) {
						prediction[j+k-maxL] += pFragment;
					}
				}
			}
		}
			
		return prediction;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new PredictFAIRESignal().instanceMain(args);
	}

}
