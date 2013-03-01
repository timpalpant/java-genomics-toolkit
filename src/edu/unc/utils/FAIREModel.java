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
	
	// Everything is final because changes would invalidate the caches
	private final float[] pNuc;
	private final float[] sonication;
	private final float crosslink;
	private final int maxL, nucSize, halfNuc, thNuc, maxNucs;
	private final Cache pOccXLNCache, pOccXLCache, pAqueousCache;

	/**
	 * Model FAIRE signal from nucleosome maps
	 * 
	 * pNuc must be normalized so that the sum of any adjacent nucSize base pairs is <= 1 
	 * (nucleosomes cannot overlap)
	 * 
	 * sonication must be normalized so that the total == 1
	 * 
	 * The model requires 2 * sonication.length overhang on each end, so the returned results
	 * will have length pNuc.length - 2*sonication.length + 1
	 */
	public FAIREModel(final float[] pNuc, final float[] sonication, final int nucSize, final float crosslink) {
		this.pNuc = pNuc;
		this.sonication = sonication;
		if (nucSize <= 0) {
			throw new IllegalArgumentException("Nucleosome size must be > 0");
		}
		this.nucSize = nucSize;
		this.halfNuc = nucSize/2;
		this.thNuc = 3*nucSize/2;
		maxL = sonication.length;
		maxNucs = maxL/nucSize + 1; // +1 because of the overhang
		log.debug("Maximum number of nucleosomes that will fit on a fragment = "+maxNucs);
		if (crosslink < 0 || crosslink > 1) {
			throw new IllegalArgumentException("Crosslinking coefficient must be in [0,1]");
		}
		// Scale the crosslinking coefficient by maxNucs
		// so that n * crosslink <= 1
		this.crosslink = crosslink / maxNucs;
		log.debug("Scaled crosslinking efficiency to maximum number of nucleosomes = "+this.crosslink);

		// Create memoization caches
		// Since pOcc() is a function on the lattice (x,l,n), where
		// x \in [nucSize/2, pNuc.length-nucSize/2]
		// l \in [minL, maxL]
		// n \in [0, maxNucs]
		// we preallocate with these dimensions: pNuc.length x (maxL+1) x (maxNucs+1)
		// This cache may be very large if pNuc or maxL is large
		pOccXLNCache = new Cache(pNuc.length, maxL+1, maxNucs+1);
		pOccXLCache = new Cache(pNuc.length, maxL+1);
		pAqueousCache = new Cache(pNuc.length, maxL+1);
		
		// Pre-calculate pOcc(x) for efficiency
		// Store it in pOccXLCache[x][0] since this is unused by pOcc
		DescriptiveStatistics occupancyStats = new DescriptiveStatistics();
		occupancyStats.setWindowSize(nucSize);
		for (int i = 0; i < pNuc.length+nucSize/2; i++) {
			if (i < pNuc.length) { 
				occupancyStats.addValue(pNuc[i]);
			}
			if (i-nucSize/2 >= 0) {
				pOccXLCache.setCache(i-nucSize/2, (float)occupancyStats.getSum());
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
			for (int x = 0; x < pNuc.length-l; x++) {
				// Calculate the probability that this fragment survives FAIRE
				// and add its probability to the prediction,
				// weighted by the sonication distribution
				float pFAIRE = sonication[l] * pAqueous(x, l);
				watson[x] += pFAIRE;
				crick[x+l-1] += pFAIRE;
			}
		}

		log.debug("Extending watson fragments");
		float[] prediction = new float[pNuc.length];
		for (int i = 0; i < pNuc.length-extend; i++) {
			for (int j = 0; j <= extend; j++) {
				prediction[i+j] += watson[i];
			}
		}
		
		log.debug("Extending crick fragments");
		for (int i = pNuc.length-1; i >= extend; i--) {
			for (int j = 0; j <= extend; j++) {
				prediction[i-j] += crick[i];
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
		float[] prediction = new float[pNuc.length];
		// Consider all possible fragment lengths
		for (int l = 1; l < sonication.length; l++) {
			// No need to count if there are no fragments of this length
			if (sonication[l] == 0) {
				continue;
			}

			// Starting at each base pair in the chunk
			for (int x = 0; x < pNuc.length-l; x++) {
				// Calculate the probability that this fragment survives FAIRE
				// and add its probability to the prediction,
				// weighted by the sonication distribution
				float pFAIRE = sonication[l] * pAqueous(x, l);
				for (int k = 0; k < l; k++) {
					prediction[x+k] += pFAIRE;
				}
			}
		}

		return prediction;
	}
	
	/**
	 * The probability that a fragment of length l, starting at x,
	 * survives extraction in the DNA phase
	 * 
	 * This does not take into account depletion by overlapping
	 */
	public float pAqueous(int x, int l) {
		if (l <= 0 || x < 0 || x+l >= pNuc.length) { 
			return 0;
		}
		
		// Check the cache
		if (pAqueousCache.isCached(x, l)) {
			return pAqueousCache.getCache(x, l);
		}
		
		// The maximum number of nucleosomes that can occupy this fragment
		int maxN = l/nucSize + 1; // +1 because of the overhang
		// Calculate the probability of being in the protein phase:
		// Calculate the probability of having each number of nucleosomes
		// Weight by the number of nucleosomes and crosslinking efficiency
		// Each event is independent because pOcc(x,l,n) is defined as having
		// exactly n nucleosomes
		float pInterphase = 0;
		for (int n = 1; n <= maxN; n++) {
			// Weighted probability of being occupied by n nucleosomes
			float pN = crosslink * n * pOcc(x, l, n);
			// See: http://lethalman.blogspot.com/2011/08/probability-of-union-of-independent.html
			pInterphase += pN * (1-pInterphase);
		}
		
		// The probability of surviving extraction in the DNA phase
		float pAqueous = 1 - pInterphase;
		pAqueousCache.setCache(x, l, pAqueous);
		
		return pAqueous;
	}
	
	/**
	 * The probability that a fragment of length l, starting at x,
	 * is occupied by exactly n nucleosomes
	 */
	public float pOcc(int x, int l, int n) {
		// Decompose the probability into two events:
		// 1) Is there a nucleosome in [x-N/2,x+N/2) and
		// 2) Is the fragment starting at x+N with length l-N occupied by n-1 nucleosomes
		
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
		if (pOccXLNCache.isCached(x, l, n)) {
			p = pOccXLNCache.getCache(x, l, n);
		} else {
			// Base case for recursion
			// the probability that it is not occupied by any nucleosomes
			if (n == 0) {
				p = 1 - pOcc(x, l);
			} else {
				// Break it down by each mutually exclusive x+j case, for j \in [-N/2,N/2)
				int start = Math.max(-halfNuc, -x);
				int stop = Math.min(halfNuc, pNuc.length-x);
				for (int j = start; j < stop; j++) {
					p += pNuc[x+j] * pOcc(x+j+thNuc, l-j-thNuc, n-1);
				}
			}
			pOccXLNCache.setCache(x, l, n, p);
		}

		return p;
	}

	/**
	 * The probability that a fragment of length l, starting at x, is occupied
	 * by at least one nucleosome, given the nucleosome distribution in pNuc
	 * 
	 * This corresponds to the base case: 1 - pOcc(x,l,0)
	 */
	public float pOcc(int x, int l) {
		// Decompose the probability into the union of two events:
		// 1) Is there a nucleosome in [x-N/2,x+N/2) or
		// 2) Is the fragment starting at x+N with length l-N occupied
		// P(1 or 2) = P(1) + P(2) - P(1 and 2)

		// Base case for recursion (the end of a fragment)
		if (l <= 0) {
			float p = 0;
			int start = Math.max(x-halfNuc, 0);
			int stop = Math.min(x+l+halfNuc, pNuc.length);
			for (int i = start; i < stop; i++) {
				p += pNuc[i];
			}
			return p;
		}
		
		// Check the cache
		if (pOccXLCache.isCached(x, l)) {
			return pOccXLCache.getCache(x, l);
		}
		
		// 1. Is there a nucleosome in [x-N/2,x+N/2)?
		// Since nucleosomes cannot overlap, these events are all independent
		// so sum the probabilities, i.e. calculate the occupancy of x
		float p1 = pOccXLCache.getCache(x);

		// 2. Is the fragment starting at x+N with length l-N occupied? Recurse
		float p2 = pOcc(x+nucSize, l-nucSize);

		// 1 and 2. What is the probability that there is a nucleosome in [x-N/2,x+N/2)
		// AND the fragment starting at x+N with length l-N is occupied?
		float p1Andp2 = 0;
		// Break it down by each mutually exclusive x+j case, for j \in [-N/2,N/2)
		int start = Math.max(-halfNuc, -x);
		int stop = Math.min(halfNuc, pNuc.length-x);
		for (int j = start; j < stop; j++) {
			p1Andp2 += pNuc[x+j] * pOcc(x+j+thNuc, l-j-thNuc);
		}

		// Store the result in the cache
		float p = p1 + p2 - p1Andp2;
		pOccXLCache.setCache(x, l, p);
		
		return p;
	}
	
	/**
	 * A memoization cache for a function that takes positive values on a lattice
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
