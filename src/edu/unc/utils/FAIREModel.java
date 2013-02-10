package edu.unc.utils;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

/**
 * Predict FAIRE from nucleosome maps
 * @author timpalpant
 *
 */
public class FAIREModel {
	
	private static final Logger log = Logger.getLogger(FAIREModel.class);
	
	private final float[] pNuc;
	private final float[] sonication;
	private final float crosslink;
	private final int maxL, nucSize, maxNucs;
	private final Cache pOccCache, pAndCache, pNCache;

	public FAIREModel(final float[] pNuc, final float[] sonication, final int nucSize, final float crosslink) {
		this.pNuc = pNuc;
		this.sonication = sonication;
		this.nucSize = nucSize;
		if (nucSize <= 0) {
			throw new IllegalArgumentException("Nucleosome size must be > 0");
		}
		if (crosslink < 0 || crosslink > 1) {
			throw new IllegalArgumentException("Crosslinking coefficient must be in [0,1]");
		}
		maxL = sonication.length;
		maxNucs = maxL/nucSize + 1; // +1 because of the overhang
		log.debug("Maximum number of nucleosomes that will fit on a fragment = "+maxNucs);
		// Scale the crosslinking coefficient by maxNucs
		// so that n * crosslink <= 1
		this.crosslink = crosslink / maxNucs;
		log.debug("Scaled crosslinking efficiency to maximum number of nucleosomes = "+this.crosslink);

		// Create memoization caches
		// Since pOcc() is a function on the lattice (x,l), where
		// x \in [nucSize/2, pNuc.length-nucSize/2]
		// l \in [minL, maxL]
		// we preallocate with these dimensions: pNuc.length x (maxL+1)
		// This cache will be large if the chunk size is too big
		pOccCache = new Cache(pNuc.length, maxL+1);
		pAndCache = new Cache(pNuc.length, maxL+1);
		pNCache = new Cache(pNuc.length, maxL+1, maxNucs+1);
		
		// Pre-calculate pOcc(x) for efficiency
		DescriptiveStatistics occupancyStats = new DescriptiveStatistics();
		occupancyStats.setWindowSize(nucSize);
		for (int i = 0; i < pNuc.length; i++) {
			occupancyStats.addValue(pNuc[i]);
			if (i-nucSize/2 >= 0) {
				pOccCache.setCache(i-nucSize/2, (float)occupancyStats.getSum());
			}
		}
	}
	
	/**
	 * Single-end prediction, with artificial uniform extension
	 * 
	 * @param chunk
	 * @param pNuc
	 * @return single-end FAIRE signal for the chunk
	 */
	public float[] singleEnd(int extend) {
		float[] watson = new float[pNuc.length];
		float[] crick = new float[pNuc.length];
		// Consider all possible fragment lengths
		for (int l = 1; l < sonication.length; l++) {
			// No need to count if there are no fragments of this length
			if (sonication[l] == 0) {
				continue;
			}

			// Starting at each base pair in the chunk
			for (int x = maxL-extend; x < pNuc.length-maxL; x++) {
				// Calculate the probability that this fragment survives FAIRE
				// and add its probability to the prediction,
				// weighted by the sonication distribution
				float pFragment = sonication[l] * pFAIRE(x, l);
				watson[x] += pFragment;
				crick[x+l-1] += pFragment;
			}
		}

		log.debug("Extending watson and crick strands to produce single-end FAIRE signal");
		float[] prediction = new float[pNuc.length-2*(maxL-1)];
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
	public float[] pairedEnd() {
		float[] prediction = new float[pNuc.length-2*(maxL-1)];
		// Consider all possible fragment lengths
		for (int l = 1; l < sonication.length; l++) {
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
		// The maximum number of nucleosomes that can occupy this fragment
		int maxN = l/nucSize + 1; // +1 because of the overhang
		// Calculate the probability of having each number of nucleosomes,
		// weight them by the number of nucleosomes,
		// and let pFAIRE be the probability that none of them happen
		// Each event is independent
		float p = 0;
		for (int n = 1; n <= maxN; n++) {
			// Weighted probability of being occupied by n nucleosomes
			float pN = crosslink * n * pOcc(x, l, n);
			// See: http://lethalman.blogspot.com/2011/08/probability-of-union-of-independent.html
			p += pN * (1-p);
		}
		
		return 1 - p;
	}
	
	/**
	 * The probability that a fragment of length l, starting at x,
	 * is occupied by exactly n nucleosomes
	 */
	private float pOcc(int x, int l, int n) {
		// Decompose the probability into two events:
		// 1) Is there a nucleosome in [x-N/2,x+N/2) and
		// 2) Is the fragment starting at x+N with length l-N occupied
		
		// Base case for recursion (the end of a fragment)
		if (l <= 0) {
			if (n == 0) {
				return 1 - pOcc(x, l);
			} else if (n == 1) {
				return pOcc(x, l);
			} else {
				return 0;
			}
		}
		
		float p = 0;
		if (pNCache.isCached(x, l, n)) {
			p = pNCache.getCache(x, l, n);
		} else {
			// Base case for recursion
			// the probability that it is not occupied by a nucleosome
			if (n == 0) {
				p = 1 - pOcc(x, l);
			} else {
				// Break it down by each mutually exclusive x+j case, for j \in [-N/2,N/2)
				for (int j = -nucSize/2; j < nucSize/2; j++) {
					p += pNuc[x+j] * pOcc(x+j+nucSize, l-j-nucSize, n-1);
				}
			}
			pNCache.setCache(x, l, n, p);
		}

		return p;
	}

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
		float p1 = pOccCache.getCache(x);

		// 2. Is the fragment starting at x+N with length l-N occupied? Recurse
		float p2 = pOcc(x+nucSize, l-nucSize);

		// 1 and 2. What is the probability that there is a nucleosome in [x-N/2,x+N/2)
		// AND the fragment starting at x+N with length l-N is occupied?
		float p1Andp2 = 0;
		if (pAndCache.isCached(x, l)) {
			p1Andp2 = pAndCache.getCache(x, l);
		} else {
			// Break it down by each mutually exclusive x+j case, for j \in [-N/2,N/2)
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
	 * A memoization cache for a function that takes values on a lattice
	 * in 1, 2, or 3D
	 */
	private static class Cache {

		private float[][][] cache;

		/** 
		 * 3D cache
		 * @param sizeX
		 * @param sizeY
		 * @param sizeZ
		 */
		public Cache(int sizeX, int sizeY, int sizeZ) {
			cache = new float[sizeX][sizeY][sizeZ];
			resetCache();
		}
		
		/**
		 * 2D cache
		 * @param sizeX
		 * @param sizeY
		 */
		public Cache(int sizeX, int sizeY) {
			this(sizeX, sizeY, 1);
		}
		
		/**
		 * 1D cache
		 * @param size
		 */
		public Cache(int size) {
			this(size, 1, 1);
		}

		public void resetCache() {
			// Set all values to -1 to mark them as uncalculated
			for (int i = 0; i < cache.length; i++) {
				for (int j = 0; j < cache[i].length; j++) {
					for (int k = 0; k < cache[i][j].length; k++) {
						cache[i][j][k] = -1;
					}
				}
			}
		}

		public boolean isCached(int x, int y, int z) {
			return cache[x][y][z] >= 0 || Float.isNaN(cache[x][y][z]);
		}
		
		public boolean isCached(int x, int y) {
			return isCached(x, y, 0);
		}
		
		public boolean isCached(int i) {
			return isCached(i, 0);
		}

		public float getCache(int x, int y, int z) {
			return cache[x][y][z];
		}
		
		public float getCache(int x, int y) {
			return getCache(x, y, 0);
		}
		
		public float getCache(int i) {
			return getCache(i, 0);
		}

		public void setCache(int x, int y, int z, float v) {
			cache[x][y][z] = v;
		}
		
		public void setCache(int x, int y, float v) {
			setCache(x, y, 0, v);
		}
		
		public void setCache(int i, float v) {
			setCache(i, 0, v);
		}

		public float density() {
			int nElements = cache.length * cache[0].length * cache[0][0].length;
			int nCached = 0;
			for (int i = 0; i < cache.length; i++) {
				for (int j = 0; j < cache[i].length; j++) {
					for (int k = 0; k < cache[i][j].length; k++) {
						if (isCached(i, j, k)) {
							nCached++;
						}
					}
				}
			}

			return ((float) nCached) / nElements;
		}
	}

}
