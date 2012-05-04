package edu.unc.utils;

/**
 * Some basic routines for calculating correlation coefficients
 * with single-precision float[] arrays
 * since commons-math only works with double[] arrays
 * 
 * @author timpalpant
 *
 */
public class FloatCorrelation {
	public static float pearson(float[] x, float[] y) {
		if (x.length != y.length) {
			throw new RuntimeException("Length of x ("+x.length+") does not equal length of y ("+y.length+")");
		}
		
		int N = 0;
		double sumX = 0, sumY = 0;
		double sumSqX = 0, sumSqY = 0;
		double sumXY = 0;
		for (int i = 0; i < x.length; i++) {
			// Skip NaN / Infinity values in the correlation calculation
			if (!Float.isNaN(x[i]) && !Float.isInfinite(x[i]) && !Float.isNaN(y[i]) && !Float.isInfinite(y[i])) {
				N++;
				sumX += x[i];
				sumY += y[i];
				sumSqX += x[i] * x[i];
				sumSqY += y[i] * y[i];
				sumXY += x[i] * y[i];
			}
		}
		
		return (float) ((N*sumXY - sumX*sumY) / Math.sqrt(N*sumSqX - sumX*sumX) / Math.sqrt(N*sumSqY - sumY*sumY));
	}
	
	public static float spearman(float[] x, float[] y) {
		if (x.length != y.length) {
			throw new RuntimeException("Length of x ("+x.length+") does not equal length of y ("+y.length+")");
		}
		
		// Compute the ranking of x and y
		float[] rankX = mapToFloat(SortUtils.rank(x));
		float[] rankY = mapToFloat(SortUtils.rank(y));
		
		return pearson(rankX, rankY);
	}
	
	private static float[] mapToFloat(int[] data) {
		float[] ret = new float[data.length];
		for (int i = 0; i < data.length; i++) {
			ret[i] = data[i];
		}
		return ret;
	}
}
