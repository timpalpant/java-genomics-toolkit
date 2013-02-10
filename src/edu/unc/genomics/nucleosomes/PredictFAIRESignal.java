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
	public int extend = 250;
	@Parameter(names = { "-n", "--nuc-size" }, description = "Nucleosome size (bp)")
	public int nucSize = 147;

	WigFileReader reader;
	float[] sonication = new float[100];
	int minL = Integer.MAX_VALUE, maxL = 0;
	DescriptiveStatistics occupancyStats = new DescriptiveStatistics();
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
		try (BufferedReader reader = Files.newBufferedReader(sonicationFile, Charset.defaultCharset())) {
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
					sonication = Arrays.copyOf(sonication, Math.max(sonication.length + 100, length + 1));
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
		log.debug("Longest fragment = "+maxL);
		if (maxL < extend) {
			maxL = extend;
		}
		// We need an additional nucSize/2 overhang
		maxL += nucSize / 2;
		// Truncate the array to the minimum possible size
		sonication = Arrays.copyOfRange(sonication, 0, maxL + 1);
		log.debug("Loaded sonication distribution for lengths: " + minL + "-" + maxL + "bp");

		// Normalize the sonication distribution so that it has total 1
		for (int i = 0; i < sonication.length; i++) {
			sonication[i] /= total;
		}

		// Calculate the maximum dyad density in any nucleosome-sized window
		// (i.e. maximum occupancy), for normalization
		// so that the dyad counts represent probabilities
		log.debug("Initializing statistics");
		occupancyStats.setWindowSize(nucSize);

		log.debug("Computing maximum genome-wide occupancy (normalization factor)");
		String maxOccChr = null;
		int maxOccPos = 0;
		for (String chr : reader.chromosomes()) {
			occupancyStats.clear();

			// Walk the chromosome while keeping track of occupancy
			int bp = reader.getChrStart(chr);
			int stop = reader.getChrStop(chr);
			while (bp <= stop) {
				int chunkStart = bp;
				int chunkStop = Math.min(chunkStart+DEFAULT_CHUNK_SIZE-1, stop);

				try {
					float[] data = reader.query(chr, chunkStart, chunkStop).getValues();
					for (int i = 0; i < data.length; i++) {
						if (Float.isNaN(data[i])) {
							data[i] = 0;
						}

						occupancyStats.addValue(data[i]);
						if (occupancyStats.getSum() > maxOcc) {
							maxOcc = (float) occupancyStats.getSum();
							maxOccChr = chr;
							maxOccPos = chunkStart + i - nucSize/2;
						}
					}
				} catch (WigFileException | IOException e) {
					log.error("Error getting data from input Wig file");
					e.printStackTrace();
					throw new CommandLineToolException("Error getting data from input Wig file");
				}

				bp = chunkStop + 1;
			}
		}
		log.debug("Found maximum genome-wide occupancy = "+maxOcc+" at "+maxOccChr+":"+maxOccPos);
	}

	@Override
	public float[] compute(Interval chunk) throws IOException, WigFileException {
		int paddedStart = Math.max(chunk.getStart() - maxL, reader.getChrStart(chunk.getChr()));
		int paddedStop = Math.min(chunk.getStop() + maxL, reader.getChrStop(chunk.getChr()));

		Contig data = reader.query(chunk.getChr(), paddedStart, paddedStop);
		float[] pNuc = data.get(chunk.getStart() - maxL, chunk.getStop() + maxL);
		// Scale the dyad density by the maximum occupancy so that it represents
		// the probability that a nucleosome is positioned at that base pair
		// You should probably remove outliers (esp. CNVs) first
		for (int i = 0; i < pNuc.length; i++) {
			pNuc[i] /= maxOcc;
		}
		
		FAIREModel model = new FAIREModel(pNuc, sonication, nucSize, crosslink);
		if (extend > 0) {
			return model.singleEnd(extend);
		} else {
			return model.pairedEnd();
		}
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
