package edu.unc.genomics;

import java.util.Comparator;

import edu.unc.genomics.ValuedInterval;
import edu.unc.genomics.io.IntervalFileFormatException;

/**
 * Represents a stereotypic nucleosome position call
 * 
 * @author timpalpant
 *
 */
public class NucleosomeCall extends ValuedInterval implements Comparable<NucleosomeCall> {

  private static final long serialVersionUID = 6522702303121259979L;

  private int dyad;
  private double dyadStdev;
  private int dyadMean;
  private double conditionalPosition;
  private int length;
  private double lengthStdev;

  /**
   * @param chr
   * @param start
   * @param stop
   */
  public NucleosomeCall(String chr, int start, int stop) {
    super(chr, start, stop);
  }

  public static NucleosomeCall parse(String line) {
    if (line.startsWith("#"))
      return null;

    String[] entry = line.split("\t");
    if (entry.length < 10) {
      throw new IntervalFileFormatException("Invalid nucleosome call has < 10 columns");
    }

    String chr = entry[0];
    int start = Integer.parseInt(entry[1]);
    int stop = Integer.parseInt(entry[2]);

    NucleosomeCall call = new NucleosomeCall(chr, start, stop);
    call.setLength(Integer.parseInt(entry[3]));
    call.setLengthStdev(Double.parseDouble(entry[4]));
    call.setDyad(Integer.parseInt(entry[5]));
    call.setDyadStdev(Double.parseDouble(entry[6]));
    call.setConditionalPosition(Double.parseDouble(entry[7]));
    call.setDyadMean(Integer.parseInt(entry[8]));
    call.setValue(Double.parseDouble(entry[9]));

    return call;
  }

  @Override
  public String toOutput() {
    return getChr() + "\t" + getStart() + "\t" + getStop() + "\t" + length() + "\t" + lengthStdev + "\t" + dyad + "\t"
        + dyadStdev + "\t" + conditionalPosition + "\t" + dyadMean + "\t" + occupancy();
  }

  /**
   * @return the dyad
   */
  public int getDyad() {
    return dyad;
  }

  /**
   * @param dyad
   *          the dyad to set
   */
  public void setDyad(int dyad) {
    this.dyad = dyad;
  }

  /**
   * @return the dyadStdev
   */
  public double getDyadStdev() {
    return dyadStdev;
  }

  /**
   * @param dyadStdev
   *          the dyadStdev to set
   */
  public void setDyadStdev(double dyadStdev) {
    this.dyadStdev = dyadStdev;
  }

  /**
   * @return the dyadMean
   */
  public int getDyadMean() {
    return dyadMean;
  }

  /**
   * @param dyadMean
   *          the dyadMean to set
   */
  public void setDyadMean(int dyadMean) {
    this.dyadMean = dyadMean;
  }

  /**
   * @return the conditionalPosition
   */
  public double getConditionalPosition() {
    return conditionalPosition;
  }

  /**
   * @param conditionalPosition
   *          the conditionalPosition to set
   */
  public void setConditionalPosition(double conditionalPosition) {
    this.conditionalPosition = conditionalPosition;
  }

  /**
   * @return the length
   */
  public int getLength() {
    return length;
  }

  /**
   * @param length
   *          the length to set
   */
  public void setLength(int length) {
    this.length = length;
  }

  /**
   * @return the lengthStdev
   */
  public double getLengthStdev() {
    return lengthStdev;
  }

  /**
   * @param lengthStdev
   *          the lengthStdev to set
   */
  public void setLengthStdev(double lengthStdev) {
    this.lengthStdev = lengthStdev;
  }

  public double occupancy() {
    return value.doubleValue();
  }

  public void setOccupancy(double value) {
    this.value = value;
  }

  @Override
  public int compareTo(NucleosomeCall o) {
    DyadComparator comparator = new DyadComparator();
    return comparator.compare(this, o);
  }

  public static class DyadComparator implements Comparator<NucleosomeCall> {

    @Override
    public int compare(NucleosomeCall o1, NucleosomeCall o2) {
      if (o1.getDyad() == o2.getDyad()) {
        return 0;
      } else if (o1.getDyad() < o2.getDyad()) {
        return -1;
      } else {
        return 1;
      }
    }

  }

}
