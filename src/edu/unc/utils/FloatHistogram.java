package edu.unc.utils;

import java.util.Arrays;

/**
 * @author timpalpant Adapted from:
 *         http://www.particle.kth.se/~fmi/kurs/PhysicsSimulation
 *         /Lectures/11B/Examples/Experiment/Histogram.java A simple histogram
 *         class. The setData(float f) finds in which bin the value falls for
 *         nBins between the given minimum and maximum values. An integer array
 *         keeps track of the number of times the input value fell into a
 *         particular bin.
 */
public class FloatHistogram {

  int[] bins = null;
  int nBins;
  double xLow, xHigh;
  double delBin;

  int overFlows = 0, underFlows = 0;

  public FloatHistogram(int nBins, double xLow, double xHigh) {

    this.nBins = nBins;
    this.xLow = xLow;
    this.xHigh = xHigh;

    bins = new int[nBins];
    delBin = (xHigh - xLow) / (float) nBins;

    reset();
  }

  public void addValue(double data) {
    if (data < xLow) {
      underFlows++;
    } else if (data >= xHigh) {
      overFlows++;
    } else {
      int bin = (int) ((data - xLow) / delBin);
      if (bin >= 0 && bin < nBins) {
        bins[bin]++;
      }
    }
  }

  public int[] getHistogram() {
    return bins;
  }

  public double getBinSize() {
    return delBin;
  }

  public void reset() {
    Arrays.fill(bins, 0);
    underFlows = 0;
    overFlows = 0;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("<" + xLow + "\t" + underFlows + "\n");
    for (int i = 0; i < bins.length; i++) {
      sb.append(xLow + i * delBin + "\t" + bins[i] + "\n");
    }
    sb.append(">" + xHigh + "\t" + overFlows);
    return sb.toString();
  }

}
