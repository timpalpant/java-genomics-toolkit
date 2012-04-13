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

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-s", "--sonication"}, description = "Sonication distribution", required = true, validateWith = ReadablePathValidator.class)
	public Path sonicationFile;
	@Parameter(names = {"-c", "--crosslinking"}, description = "FAIRE efficiency / crosslinking coefficient")
	public double crosslink = 1;

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
		
		log.debug("Computing forward sums");
		float[][] fsums = new float[sonication.length][result.length];
		// TODO Compute this more efficiently
		for (int i = 1; i < sonication.length; i++) {
			for (int x = 0; x < result.length-i+1; x++) {
				for (int k = 0; k < i; k++) {
					fsums[i][x] += crosslink*result[x+k];
				}
				fsums[i][x] = 1 - Math.min(fsums[i][x], 1);
			}
		}
		
		log.debug("Computing FAIRE prediction");
		float[] prediction = new float[stop-start+1];
		// TODO Compute this more efficiently
		for (int x = 0; x < prediction.length; x++) {
			for (int i = 1; i < sonication.length; i++) {
				for (int j = -i+1; j <= 0; j++) {
          // Add to total and weight by relative abundance of this fragment length
          prediction[x] += sonication[i] * fsums[i][x+j+L];
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
