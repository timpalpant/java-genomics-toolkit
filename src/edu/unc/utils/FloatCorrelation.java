package edu.unc.utils;

/**
 * Some basic routines for calculating correlation coefficients with
 * single-precision float[] arrays since commons-math only works with double[]
 * arrays
 * 
 * @author timpalpant
 *
 */
public class FloatCorrelation {
  /**
   * Calculate Pearson's product-moment correlation coefficient (R) between x,y
   * data
   * 
   * @param x
   *          a vector of values
   * @param y
   *          a vector of values
   * @return the Pearson correlation between the values in x and the values in y
   */
  public static float pearson(float[] x, float[] y) {
    if (x.length != y.length) {
      throw new RuntimeException("Length of x (" + x.length + ") does not equal length of y (" + y.length + ")");
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

    return (float) ((N * sumXY - sumX * sumY) / Math.sqrt(N * sumSqX - sumX * sumX) / Math.sqrt(N * sumSqY - sumY
        * sumY));
  }

  /**
   * Calculate Spearman's rank correlation coefficient between x,y data defined
   * to be the Pearson correlation between the ranks of the data
   * 
   * @param x
   *          a vector of values
   * @param y
   *          a vector of values
   * @return the Spearman correlation between the values in x and the values in
   *         y
   */
  public static float spearman(float[] x, float[] y) {
    if (x.length != y.length) {
      throw new RuntimeException("Length of x (" + x.length + ") does not equal length of y (" + y.length + ")");
    }

    // Compute the ranking of x and y
    float[] rankX = ArrayUtils.mapToFloat(SortUtils.rank(x));
    float[] rankY = ArrayUtils.mapToFloat(SortUtils.rank(y));

    return pearson(rankX, rankY);
  }

}
