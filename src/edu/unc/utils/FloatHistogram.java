package edu.unc.utils;

////********************************************************************
// Histogram.java 
// Adapted from: http://www.particle.kth.se/~fmi/kurs/PhysicsSimulation/Lectures/11B/Examples/Experiment/Histogram.java
//
// A simple histogram class. The setData(float f) finds in which bin
// the value falls for nBins between the given minimum and maximum
// values. An integer array keeps track of the number of times the input
// value fell into a particular bin.
// The DataChart class is used to display the histogram. This class
// in turn uses the BarChart tool which needs the data passed as a 
// string array. This is not optimal but OK for demonstration purposes.
//
//********************************************************************
public class FloatHistogram {

	int[] bins = null;
	int nBins;
	float xLow, xHigh;
	float delBin;

	int overFlows = 0, underFlows = 0;

	public FloatHistogram(int nBins, float xLow, float xHigh) {

		this.nBins = nBins;
		this.xLow = xLow;
		this.xHigh = xHigh;

		bins = new int[nBins];
		delBin = (xHigh - xLow) / (float) nBins;

		reset();
	}

	public FloatHistogram(int nBins, double xLow, double xHigh) {
		this(nBins, (float) xLow, (float) xHigh);
	}

	public void addValue(double data) {
		addValue((float) data);
	}

	public void addValue(float data) {
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
	
	public float getBinSize() {
		return delBin;
	}

	public void reset() {
		for (int i = 0; i < nBins; i++) {
			bins[i] = 0;
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bins.length; i++) {
			sb.append(xLow+i*delBin + "\t" + bins[i] + "\n");
		}
		return sb.toString();
	}

}
