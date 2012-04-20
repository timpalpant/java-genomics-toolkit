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

public class PredictFAIRESignal extends WigMathTool {

	private static final Logger log = Logger.getLogger(PredictFAIRESignal.class);

	@Parameter(names = {"-i", "--input"}, description = "Input (nucleosome occupancy)", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-s", "--sonication"}, description = "Sonication distribution", required = true, validateWith = ReadablePathValidator.class)
	public Path sonicationFile;
	@Parameter(names = {"-c", "--crosslinking"}, description = "FAIRE efficiency / crosslinking coefficient")
	public float crosslink = 1;
	@Parameter(names = {"-n", "--nucSize"}, description = "Nucleosome size (bp)")
	public int nucSize = 147;
	@Parameter(names = {"-x", "--extend"}, description = "In silico read extension (bp)")
	public int extend = 250;

	double[] sonication = new double[100];
	int L;
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
				int length = Integer.parseInt(entry[0]);
				double percent = Double.parseDouble(entry[1]);
				// Expand the sonication distribution array if necessary
				if (length >= sonication.length) {
					sonication = Arrays.copyOf(sonication, Math.max(sonication.length+100, length+1));
				}
				if (length > L) {
					L = length;
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
		sonication = Arrays.copyOfRange(sonication, 0, L);
		log.debug("Loaded sonication distribution for lengths: 0-"+L+"bp");
		
		// Normalize the sonication distribution so that it has total 1
		for (int i = 0; i < sonication.length; i++) {
			sonication[i] /= total;
		}
		
		// Store the maximum occupancy
		maxOcc = inputFile.max();
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		int paddedStart = Math.max(start-L, inputFile.getChrStart(chr));
		int paddedStop = Math.min(stop+L, inputFile.getChrStop(chr));
		
		Iterator<WigItem> data = inputFile.query(chr, paddedStart, paddedStop);
		float[] result = WigFile.flattenData(data, start-L, stop+L, 0);
		
		// Scale the occupancy by the maximum occupancy so that it represents
		// the probability that a base pair is occupied by a nucleosome
		for (int i = 0; i < result.length; i++) {
			result[i] /= maxOcc;
		}
		
		log.debug("Computing FAIRE prediction");
		float[] watson = new float[result.length];
		float[] crick = new float[result.length];
		// Consider all possible fragment lengths
		for (int i = 1; i < sonication.length; i++) {
			// Starting at each base pair in the chunk
			for (int j = 0; j < result.length-i; j++) {
				// Calculate the probability that this fragment is occupied by a nucleosome
				// using the inclusion-exclusion principle with nucleosome width = 147bp;
				float pOccupied = 0;
				for (int k = j; k < j+i; k++) {
					pOccupied += result[k];
				}
				// Calculate the probability that this fragment survives FAIRE
				float pFAIRE = crosslink*(1-pOccupied);
				
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
				if (i+j-L > 0 && i+j-L < prediction.length) {
					prediction[i+j-L] += watson[i];
				}
				
				// Extend on the - strand
				if (i-j-L > 0 && i-j-L < prediction.length) {
					prediction[i-j-L] += crick[i];
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
