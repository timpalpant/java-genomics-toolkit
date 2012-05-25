package edu.unc.utils;

/**
 * Miscellaneous utility functions for working with arrays
 * @author timpalpant
 *
 */
public class ArrayUtils {
	
	/**
	 * Get the index of the maximum (largest) value in an array
	 * In the event of a tie, the first index is returned
	 * @param x a vector of values
	 * @return the index of the largest element in x
	 */
	public static int maxIndex(float[] x) {
		float maxValue = -Float.MAX_VALUE;
		int maxIndex = -1;
		for (int i = 0; i < x.length; i++) {
			if (x[i] > maxValue) {
				maxValue = x[i];
				maxIndex = i;
			}
		}
		
		return maxIndex;
	}
}
