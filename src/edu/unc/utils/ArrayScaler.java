package edu.unc.utils;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

/**
 * Generate scaled versions of an array with different resolution Can be used
 * for downsampling/upsampling a 1D array using interpolation Interpolation
 * routines are from Apache commons-math3
 * 
 * @author timpalpant
 *
 */
public class ArrayScaler {

  private UnivariateFunction interp;

  /**
   * Create a new ArrayScaler
   * 
   * @param x
   *          the seed array to downsample/upsample
   */
  public ArrayScaler(double[] x) {
    double[] indices = new double[x.length];
    for (int i = 0; i < indices.length; i++) {
      indices[i] = ((double) i) / (x.length - 1);
    }

    UnivariateInterpolator interpolator = new SplineInterpolator();
    interp = interpolator.interpolate(indices, x);
  }

  /**
   * Interpolate to create a new scaled vector of length l
   * 
   * @param l
   *          the desired vector length
   * @return a new vector of length l created by interpolating x
   */
  public double[] getScaled(int l) {
    double[] stretched = new double[l];
    for (int i = 0; i < l; i++) {
      stretched[i] = interp.value(((double) i) / l);
    }
    return stretched;
  }
}
