package edu.unc.genomics;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.math3.stat.clustering.Clusterable;

/**
 * @author timpalpant Holds a row of data for the KMeans program
 */
public class KMeansRow implements Clusterable<KMeansRow>, Serializable {

  private static final long serialVersionUID = -323598431692368500L;

  private final String id;
  /** Point coordinates. */
  private final float[] point;

  /**
   * Build an instance wrapping an float array.
   * <p>
   * The wrapped array is referenced, it is <em>not</em> copied.
   * </p>
   * 
   * @param point
   *          the n-dimensional point in integer space
   */
  public KMeansRow(final String id, final float[] point) {
    this.point = point;
    this.id = id;
  }

  /**
   * Get the n-dimensional point in float space.
   * 
   * @return a reference (not a copy!) to the wrapped array
   */
  public float[] getPoint() {
    return point;
  }

  /** {@inheritDoc} */
  public double distanceFrom(final KMeansRow p) {
    double sumSquares = 0;
    float[] otherPoint = p.getPoint();
    for (int i = 0; i < point.length; i++) {
      sumSquares += Math.pow(point[i] - otherPoint[i], 2);
    }
    return Math.sqrt(sumSquares);
  }

  /** {@inheritDoc} */
  public KMeansRow centroidOf(final Collection<KMeansRow> points) {
    float[] centroid = new float[getPoint().length];
    for (KMeansRow p : points) {
      for (int i = 0; i < centroid.length; i++) {
        centroid[i] += p.getPoint()[i];
      }
    }
    for (int i = 0; i < centroid.length; i++) {
      centroid[i] /= points.size();
    }
    return new KMeansRow(id, centroid);
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof KMeansRow)) {
      return false;
    }
    final float[] otherPoint = ((KMeansRow) other).getPoint();
    if (point.length != otherPoint.length) {
      return false;
    }
    for (int i = 0; i < point.length; i++) {
      if (point[i] != otherPoint[i]) {
        return false;
      }
    }
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    int hashCode = 0;
    for (Float i : point) {
      hashCode += i.hashCode() * 13 + 7;
    }
    return hashCode;
  }

  /**
   * {@inheritDoc}
   * 
   * @since 2.1
   */
  @Override
  public String toString() {
    final StringBuilder buff = new StringBuilder(id);
    for (float value : getPoint()) {
      buff.append("\t").append(value);
    }
    return buff.toString();
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

}
