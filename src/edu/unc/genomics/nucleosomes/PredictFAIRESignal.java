package edu.unc.genomics.nucleosomes;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.wigmath.WigMathTool;
import edu.unc.utils.InclusionExclusion;

public class PredictFAIRESignal extends WigMathTool {

	private static final Logger log = Logger.getLogger(PredictFAIRESignal.class);

	@Parameter(names = {"-i", "--input"}, description = "Input (nucleosome occupancy)", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-s", "--sonication"}, description = "Sonication distribution", required = true, validateWith = ReadablePathValidator.class)
	public Path sonicationFile;
	@Parameter(names = {"-c", "--crosslinking"}, description = "FAIRE efficiency / crosslinking coefficient")
	public float crosslink = 1;
	@Parameter(names = {"-x", "--extend"}, description = "In silico read extension (bp)")
	public int extend = 250;

	double[] sonication = new double[100];
	int minL = Integer.MAX_VALUE, maxL = 0;
	double maxOcc;
	
	@Override
	public void setup() {
		inputs.add(inputFile);
		
		log.debug("Loading sonication fragment length distribution");
		double total = 0;
		try(BufferedReader reader = Files.newBufferedReader(sonicationFile, Charset.defaultCharset())) {
			String line;
			while ((line = reader.readLine()) != null) {
				// Parse the line
				String[] entry = line.split("\t");
				if (entry.length != 2) {
					throw new CommandLineToolException("Invalid format for sonication distribution file");
				}
				int length = Integer.parseInt(entry[0]);
				double percent = Double.parseDouble(entry[1]);
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
		log.debug("Loaded sonication distribution for lengths: 0-"+maxL+"bp");
		
		// Normalize the sonication distribution so that it has total 1
		for (int i = 0; i < sonication.length; i++) {
			sonication[i] /= total;
		}
		
		// Store the maximum occupancy
		maxOcc = inputFile.max();
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		int paddedStart = Math.max(start-maxL, inputFile.getChrStart(chr));
		int paddedStop = Math.min(stop+maxL, inputFile.getChrStop(chr));
		
		Iterator<WigItem> data = inputFile.query(chr, paddedStart, paddedStop);
		float[] result = WigFile.flattenData(data, start-maxL, stop+maxL, 0);
		
		// Scale the occupancy by the maximum occupancy so that it represents
		// the probability that a base pair is occupied by a nucleosome
		for (int i = 0; i < result.length; i++) {
			result[i] /= maxOcc;
		}
		
		log.debug("Computing FAIRE prediction");
		float[] watson = new float[result.length];
		float[] crick = new float[result.length];
		// Consider all possible fragment lengths
		for (int i = minL; i < sonication.length; i++) {
			// No need to count if there are no fragments of this length
			if (sonication[i] == 0) {
				continue;
			}
			
			// Starting at each base pair in the chunk
			for (int j = 0; j < result.length-i; j++) {
				// Calculate the probability that this fragment is occupied by a nucleosome
				// using the inclusion-exclusion principle with nucleosome width = 147bp;
				float pOccupied = InclusionExclusion.independent(result, j, j+i);
				// Calculate the probability that this fragment survives FAIRE
				float pFAIRE = 1 - crosslink*pOccupied;
				
				// Add its probability at the +/- ends, weighted by fragment abundance
				watson[j] += sonication[i]*pFAIRE;
				crick[j+i-1] += sonication[i]*pFAIRE;
			}
		}
		
		log.debug("Extending reads from the +/- strands");
		float[] prediction = new float[stop-start+1];
		for (int i = 0; i < result.length; i++) {
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
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new PredictFAIRESignal().instanceMain(args);
	}

}
