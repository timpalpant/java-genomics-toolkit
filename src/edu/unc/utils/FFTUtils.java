package edu.unc.utils;

/**
 * Routines for working with Fourier transforms and frequency spectrum data
 * 
 * @author timpalpant
 *
 */
public class FFTUtils {
  /**
   * Computes the power spectrum from FFT data taking into account even/odd
   * length arrays refer to JTransforms documentation for layout of the FFT data
   * 
   * @param f
   *          the DFT-transformed data from JTransforms.realForward()
   * @return the power spectrum of the complex frequency spectrum in f
   */
  public static float[] abs2(float[] f) {
    int n = f.length;
    float[] ps = new float[n / 2 + 1];
    // DC component
    ps[0] = (f[0] * f[0]) / (n * n);

    // Even
    if (n % 2 == 0) {
      for (int k = 1; k < n / 2; k++) {
        ps[k] = f[2 * k] * f[2 * k] + f[2 * k + 1] * f[2 * k + 1];
      }
      ps[n / 2] = f[1] * f[1];
      // Odd
    } else {
      for (int k = 1; k < (n - 1) / 2; k++) {
        ps[k] = f[2 * k] * f[2 * k] + f[2 * k + 1] * f[2 * k + 1];
      }

      ps[(n - 1) / 2] = f[n - 1] * f[n - 1] + f[1] * f[1];
    }

    return ps;
  }

  /**
   * Computes the autocovariance of the data in f
   * 
   * @param x
   *          a vector of real data
   * @param maxShift
   *          the maximum phase shift to calculate
   * @return the autocovariance values, having length Math.min(x.length,
   *         maxShift)
   */
  public static float[] autocovariance(float[] x, int maxShift) {
    float total = 0;
    for (int i = 0; i < x.length; i++) {
      total += x[i];
    }
    float mean = total / x.length;

    int stop = Math.min(x.length, maxShift);
    float[] auto = new float[stop];
    for (int i = 0; i < stop; i++) {
      for (int j = 0; j < x.length - i; j++) {
        auto[i] += (x[j] - mean) * (x[j + i] - mean);
      }
    }

    return auto;
  }

  /**
   * Computes the autocovariance of the data in f for all possible shifts
   * 
   * @param x
   *          a vector of real data
   * @return the autocovariance values, having length equal to x.length
   */
  public static float[] autocovariance(float[] x) {
    return autocovariance(x, x.length);
  }

}
