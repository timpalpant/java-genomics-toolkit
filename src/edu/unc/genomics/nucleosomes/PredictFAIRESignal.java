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

/**
 * Attempt to predict FAIRE signal from nucleosome occupancy data using a simple
 * probabilistic model
 * 
 * @author timpalpant
 * 
 */
public class PredictFAIRESignal extends WigMathTool {

	private static final Logger log = Logger.getLogger(PredictFAIRESignal.class);

	@Parameter(names = { "-i", "--input" }, description = "Nucleosome dyad density (Wig)", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = { "-s", "--sonication" }, description = "Sonication distribution", required = true, validateWith = ReadablePathValidator.class)
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
	float[] pNuc, pOcc;
	Cache pOccCache, pAndCache;

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
		pNuc = data.get(chunk.getStart() - maxL, chunk.getStop() + maxL);

		// Scale the dyad density by the maximum occupancy so that it represents
		// the probability that a nucleosome is positioned at that base pair
		// You should probably remove outliers (esp. CNVs) first
		// Also save the occupancy at each base pair (for efficiency)
		occupancyStats.clear();
		pOcc = new float[pNuc.length];
		for (int i = 0; i < pNuc.length; i++) {
			pNuc[i] /= maxOcc;
			occupancyStats.addValue(pNuc[i]);
			if (i - nucSize / 2 >= 0) {
				pOcc[i - nucSize / 2] = (float) occupancyStats.getSum();
			}
		}

		// Reset the memoization caches since we updated pNuc and pOcc
		// Since pOcc() is a function on the lattice (x,l), where
		// x \in [nucSize/2, pNuc.length-nucSize/2]
		// l \in [minL, maxL]
		// we preallocate with these dimensions: pNuc.length x (maxL+1)
		// This cache will be large if the chunk size is too big
		pOccCache = new Cache(pNuc.length, maxL + 1);
		pAndCache = new Cache(pNuc.length, maxL + 1);

		float[] prediction;
		if (extend < 0) {
			prediction = paired(chunk);
		} else {
			prediction = single(chunk);
		}

		return prediction;
	}

	/**
	 * Single-end prediction, with artificial uniform extension
	 * 
	 * @param chunk
	 * @param pNuc
	 * @return single-end FAIRE signal for the chunk
	 */
	private float[] single(Interval chunk) {
		float[] watson = new float[pNuc.length];
		float[] crick = new float[pNuc.length];
		// Consider all possible fragment lengths
		for (int l = minL; l < sonication.length; l++) {
			// No need to count if there are no fragments of this length
			if (sonication[l] == 0) {
				continue;
			}

			// Starting at each base pair in the chunk
			for (int x = maxL - extend; x < pNuc.length - maxL; x++) {
				// Calculate the probability that this fragment survives FAIRE
				// and add its probability to the prediction,
				// weighted by the sonication distribution
				float pFragment = sonication[l] * pFAIRE(x, l);
				watson[x] += pFragment;
				crick[x+l-1] += pFragment;
			}
		}

		log.debug("Extending watson and crick strands to produce single-end FAIRE signal");
		float[] prediction = new float[chunk.length()];
		for (int i = 0; i < pNuc.length; i++) {
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
	 * 
	 * @return paired-end FAIRE signal for the chunk
	 */
	private float[] paired(Interval chunk) {
		float[] prediction = new float[chunk.length()];
		// Consider all possible fragment lengths
		for (int l = minL; l < sonication.length; l++) {
			// No need to count if there are no fragments of this length
			if (sonication[l] == 0) {
				continue;
			}

			// Starting at each base pair in the chunk
			for (int x = maxL-l; x < pNuc.length-maxL; x++) {
				// Calculate the probability that this fragment survives FAIRE
				// and add its probability to the prediction,
				// weighted by the sonication distribution
				float pFragment = sonication[l] * pFAIRE(x, l);
				int start = Math.max(x-maxL, 0);
				int stop = Math.min(x-maxL+l, prediction.length);
				for (int k = start; k < stop; k++) {
					prediction[k] += pFragment;
				}
			}
		}

		return prediction;
	}

	/**
	 * The probability that a fragment of length l, starting at x, survives
	 * phenol:chloroform, given the nucleosome distribution in pNuc
	 */
	private float pFAIRE(int x, int l) {
		return 1 - crosslink * pOcc(x, l);
	}
	
	/**
	 * The probability that a fragment of length l, starting at x,
	 * is occupied by n nucleosomes
	 */
	//private float pOcc(int x, int l, int n) {
		
	//a}

	/**
	 * The probability that a fragment of length l, starting at x, is occupied
	 * by a nucleosome, given the nucleosome distribution in pNuc
	 * 
	 * pNuc[x] must be normalized so that the sum of any adjacent N base pairs
	 * is <= 1 (i.e. nucleosomes cannot overlap)
	 * 
	 * pOcc[x] must be the convolution of pNuc with an N-base pair window, i.e.
	 * the probability of being occupied for each base pair x
	 */
	private float pOcc(int x, int l) {
		// Decompose the probability into the union of two events:
		// 1) Is there a nucleosome in [x-N/2,x+N/2) or
		// 2) Is the fragment starting at x+N with length l-N occupied
		// P(1 or 2) = P(1) + P(2) - P(1 and 2)

		// Base case for recursion (the end of a fragment)
		if (l <= 0) {
			float p = 0;
			// log.debug("x = "+x+", l = "+l);
			for (int i = x-nucSize/2; i < x+l+nucSize/2; i++) {
				p += pNuc[i];
			}
			return p;
		}

		// Check for a cached result
		if (pOccCache.isCached(x, l)) {
			return pOccCache.getCache(x, l);
		}

		// 1. Is there a nucleosome in [x-N/2,x+N/2)?
		// Since nucleosomes cannot overlap, these events are all independent
		// so sum the probabilities, i.e. calculate the occupancy of x
		float p1 = pOcc[x];

		// 2. Is the fragment starting at x+N with length l-N occupied? Recurse
		float p2 = pOcc(x+nucSize, l-nucSize);

		// 1 and 2. What is the probability that there is a nucleosome in [x-N/2,x+N/2)
		// AND the fragment starting at x+N with length l-N is occupied?
		// Break it down by each mutually exclusive x+j case, for j \in
		// [-N/2,N/2)
		float p1Andp2 = 0;
		if (pAndCache.isCached(x, l)) {
			p1Andp2 = pAndCache.getCache(x, l);
		} else {
			for (int j = -nucSize/2; j < nucSize/2; j++) {
				p1Andp2 += pNuc[x+j] * pOcc(x+j+nucSize, l-j-nucSize);
			}
			pAndCache.setCache(x, l, p1Andp2);
		}

		// Cache the result
		float p = p1 + p2 - p1Andp2;
		pOccCache.setCache(x, l, p);

		return p;
	}

	/**
	 * @param args
	 * @throws WigFileException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new PredictFAIRESignal().instanceMain(args);
	}

	/**
	 * A memoization cache for a function that takes values on a 2D lattice in
	 * \mathbb{N}^2
	 */
	private static class Cache {

		private float[][] cache;

		public Cache(int sizeX, int sizeY) {
			cache = new float[sizeX][sizeY];
			resetCache();
		}

		public void resetCache() {
			// Set all values to -1 to mark them as uncalculated
			for (int i = 0; i < cache.length; i++) {
				for (int j = 0; j < cache[i].length; j++) {
					cache[i][j] = -1;
				}
			}
		}

		public boolean isCached(int x, int y) {
			return cache[x][y] >= 0 || Float.isNaN(cache[x][y]);
		}

		public float getCache(int x, int y) {
			return cache[x][y];
		}

		public void setCache(int x, int y, float v) {
			cache[x][y] = v;
		}

		public float density() {
			int nElements = cache.length * cache[0].length;
			int nCached = 0;
			for (int i = 0; i < cache.length; i++) {
				for (int j = 0; j < cache[i].length; j++) {
					if (isCached(i, j)) {
						nCached++;
					}
				}
			}

			return ((float) nCached) / nElements;
		}
	}

}
