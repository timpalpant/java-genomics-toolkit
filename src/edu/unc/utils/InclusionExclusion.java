package edu.unc.utils;

/**
 * Algorithms for computing inclusion-exclusion probabilities
 * @author timpalpant
 *
 */
public class InclusionExclusion {
	
	/**
	 * Compute the probability of a union of independent events
	 * @param probabilities the probability of each independent event
	 * @param start the index of the first entry to consider
	 * @param stop the index of the last entry to consider (not inclusive)
	 * @return the probability of any one of the events occurring
	 */
	public static float independent(float[] probabilities, int start, int stop) {
		float p = 0;
		
		// See: http://lethalman.blogspot.com/2011/08/probability-of-union-of-independent.html
		for (int i = start; i < stop; i++) {
			p += probabilities[i] * (1-p);
		}
		
		return p;
	}
	
	/**
	 * Compute the probability of a union of independent events
	 * @param probabilities the probability of each independent event
	 * @return the probability of any one of the events occurring
	 */
	public static float independent(float[] probabilities) {
		return independent(probabilities, 0, probabilities.length);
	}
}
