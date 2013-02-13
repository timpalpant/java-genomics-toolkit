package edu.unc.genomics.nucleosomes;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.WigMathTool;
import edu.unc.genomics.io.WigFileFormatException;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.FAIREModel;

/**
 * Attempt to predict FAIRE signal from nucleosome occupancy data using a simple
 * probabilistic model
 * 
 * @author timpalpant
 * 
 */
public class PredictFAIRESignal extends WigMathTool {

	private static final Logger log = Logger.getLogger(PredictFAIRESignal.class);

	@Parameter(names = { "-i", "--input" }, description = "Nucleosome dyad density (Wig)", 
			   required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = { "-s", "--sonication" }, description = "Sonication distribution", 
			   required = true, validateWith = ReadablePathValidator.class)
	public Path sonicationFile;
	@Parameter(names = { "-e", "--efficiency" }, description = "FAIRE crosslinking efficiency [0,1]")
	public float crosslink = 1;
	@Parameter(names = { "-x", "--extend" }, description = "Single-end read extension (bp); -1 for paired-end")
	public int extend = -1;
	@Parameter(names = { "-n", "--nuc-size" }, description = "Nucleosome size (bp)")
	public int nucSize = 147;

	WigFileReader reader;
	float[] sonication = new float[100];
	float maxOcc;

	public static float[] loadSonication(Path s) {
		log.debug("Loading sonication fragment length distribution");
		float[] sonication = new float[100];
		int maxL = 0;
		float total = 0;
		try (BufferedReader reader = Files.newBufferedReader(s, Charset.defaultCharset())) {
			String line;
			while ((line = reader.readLine()) != null) {
				// Parse the line
				String[] entry = line.split("\t");
				if (entry.length != 2) {
					throw new CommandLineToolException(
							"Invalid format for sonication distribution file (length\tpercent)");
				}
				int length = Integer.parseInt(entry[0]);
				float percent = Float.parseFloat(entry[1]);
				// Expand the sonication distribution array if necessary
				if (length >= sonication.length) {
					sonication = Arrays.copyOf(sonication, Math.max(sonication.length+100, length+1));
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
		
		log.debug("Longest fragment length: "+maxL+"bp");
		// Truncate the array to the minimum possible size
		sonication = Arrays.copyOfRange(sonication, 0, maxL+1);

		// Normalize the sonication distribution so that it has total 1
		for (int i = 0; i < sonication.length; i++) {
			sonication[i] /= total;
		}
		
		return sonication;
	}
	
	/**
	 * Calculate the maximum dyad density in any nucleosome-sized window
	 * (i.e. maximum occupancy), for normalization
	 * so that the dyad counts represent probabilities
	 * @throws IOException 
	 * @throws WigFileFormatException 
	 * @throws WigFileException 
	 */
	public static float getMaxOccupancy(WigFileReader reader, int nucSize) 
			throws WigFileFormatException, IOException, WigFileException {
		log.debug("Computing maximum genome-wide occupancy");
		DescriptiveStatistics occupancyStats = new DescriptiveStatistics();
		occupancyStats.setWindowSize(nucSize);

		float maxOcc = 0;
		String maxOccChr = null;
		int maxOccPos = 0;
		for (String chr : reader.chromosomes()) {
			occupancyStats.clear();

			// Walk the chromosome while keeping track of occupancy
			int start = reader.getChrStart(chr);
			int stop = reader.getChrStop(chr);
			float[] data = reader.query(chr, start, stop).getValues();
			for (int i = 0; i < data.length; i++) {
				if (Float.isNaN(data[i])) {
					data[i] = 0;
				}

				occupancyStats.addValue(data[i]);
				if (occupancyStats.getSum() > maxOcc) {
					maxOcc = (float) occupancyStats.getSum();
					maxOccChr = chr;
					maxOccPos = i - nucSize/2;
				}
			}
		}
		
		log.debug("Found maximum genome-wide occupancy = "+maxOcc+" at "+maxOccChr+":"+maxOccPos);
		return maxOcc;
	}
	
	@Override
	public void setup() {
		try {
			reader = WigFileReader.autodetect(inputFile);
			maxOcc = getMaxOccupancy(reader, nucSize);
		} catch (IOException | WigFileFormatException | WigFileException e) {
			throw new CommandLineToolException(e);
		}
		addInputFile(reader);

		sonication = loadSonication(sonicationFile);
	}

	@Override
	public float[] compute(Interval chunk) throws IOException, WigFileException {
		int paddedStart = Math.max(chunk.getStart()-sonication.length, reader.getChrStart(chunk.getChr()));
		int paddedStop = Math.min(chunk.getStop()+sonication.length, reader.getChrStop(chunk.getChr()));

		Contig data = reader.query(chunk.getChr(), paddedStart, paddedStop);
		float[] pNuc = data.get(chunk.getStart()-sonication.length, chunk.getStop()+sonication.length);
		// Scale the dyad density by the maximum occupancy so that it represents
		// the probability that a nucleosome is positioned at that base pair
		// You should probably remove outliers (esp. CNVs) first
		for (int i = 0; i < pNuc.length; i++) {
			pNuc[i] /= maxOcc;
		}
		
		FAIREModel model = new FAIREModel(pNuc, sonication, nucSize, crosslink);
		float[] prediction;
		if (extend > 0) {
			prediction = model.singleEnd(extend);
		} else {
			prediction = model.pairedEnd();
		}
		
		return Arrays.copyOfRange(prediction, sonication.length, prediction.length-sonication.length);
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
