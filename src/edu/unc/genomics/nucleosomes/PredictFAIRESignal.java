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

	double[] sonication = new double[100];
	int L;
	
	@Override
	public void setup() {
		inputs.add(inputFile);
		
		log.debug("Loading sonication fragment length distribution");
		double total = 0;
		try(BufferedReader reader = Files.newBufferedReader(sonicationFile, Charset.defaultCharset())) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] entry = line.split("\t");
				int length = Integer.parseInt(entry[0]);
				double percent = Double.parseDouble(entry[1]);
				if (length >= sonication.length) {
					sonication = Arrays.copyOf(sonication, Math.max(sonication.length+100, length+1));
				}
				sonication[length] = percent;
				total += percent;
			}
		} catch (IOException e) {
			log.fatal("Error loading sonication fragment length distribution");
			e.printStackTrace();
			throw new CommandLineToolException("Error loading sonication fragment length distribution");
		}
		
		// Normalize the sonication distribution so that it has total 1
		for (int i = 0; i < sonication.length; i++) {
			sonication[i] /= total;
		}
		
		L = sonication.length-1;
		log.debug("Loaded sonication distribution for lengths: 0-"+L+"bp");
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		int paddedStart = Math.max(start-L, inputFile.getChrStart(chr));
		int paddedStop = Math.min(start+L, inputFile.getChrStop(chr));
		
		Iterator<WigItem> data = inputFile.query(chr, paddedStart, paddedStop);
		float[] result = WigFile.flattenData(data, start-L, stop+L, 0);
		
		log.debug("Computing forward sums");
		float[][] fsums = new float[L+1][result.length];
		for (int i = 1; i <= L; i++) {
			for (int x = 0; x < result.length-i+1; x++) {
				for (int k = 0; k < i; k++) {
					fsums[i][x] += result[x+k];
				}
			}
		}
		
		log.debug("Computing FAIRE prediction");
		float[] prediction = new float[stop-start+1];
		for (int x = 0; x < prediction.length; x++) {
			for (int i = 1; i <= L; i++) {
				for (int j = -i+1; j <= 0; j++) {
					// Probability that this state is occupied by a nucleosome
          float pOcc = Math.min(fsums[i][x+j+L], 1);
          // Probability that this state survives FAIRE
          float pFaire = 1 - pOcc;
          // Add to total and weight by relative abundance of this fragment length
          prediction[i] += sonication[i] * pFaire;
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
