package edu.unc.utils;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class FloatCorrelationTest {

  private static final float[] A = { 0.1f, -4, 0.5f, 12, -3, -4.2f, 12.2f, 10.1f };
  private static final float[] B = { 5, 4, 3, 2, 1, 0, -2.2f, 1.3f };

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testPearson() {
    // Arrays should correlate perfectly with themselves
    assertEquals(1.0, FloatCorrelation.pearson(A, A), 1e-7);
    assertEquals(1.0, FloatCorrelation.pearson(B, B), 1e-7);

    // And anticorrelate perfectly with their negatives
    assertEquals(-1.0, FloatCorrelation.pearson(A, neg(A)), 1e-7);
    assertEquals(-1.0, FloatCorrelation.pearson(B, neg(B)), 1e-7);

    assertEquals(-0.417556240272523, FloatCorrelation.pearson(A, B), 1e-7);
    assertEquals(-0.417556240272523, FloatCorrelation.pearson(B, A), 1e-7);
    assertEquals(0.417556240272523, FloatCorrelation.pearson(A, neg(B)), 1e-7);
    assertEquals(0.417556240272523, FloatCorrelation.pearson(neg(A), B), 1e-7);
    assertEquals(-0.417556240272523, FloatCorrelation.pearson(neg(A), neg(B)), 1e-7);
  }

  @Test
  public void testSpearman() {
    // Arrays should correlate perfectly with themselves
    assertEquals(1.0, FloatCorrelation.spearman(A, A), 1e-7);
    assertEquals(1.0, FloatCorrelation.spearman(B, B), 1e-7);

    // And anticorrelate perfectly with their negatives
    assertEquals(-1.0, FloatCorrelation.spearman(A, neg(A)), 1e-7);
    assertEquals(-1.0, FloatCorrelation.spearman(B, neg(B)), 1e-7);

    assertEquals(-0.190476190476190, FloatCorrelation.spearman(A, B), 1e-7);
    assertEquals(-0.190476190476190, FloatCorrelation.spearman(B, A), 1e-7);
    assertEquals(0.190476190476190, FloatCorrelation.spearman(A, neg(B)), 1e-7);
    assertEquals(0.190476190476190, FloatCorrelation.spearman(neg(A), B), 1e-7);
    assertEquals(-0.190476190476190, FloatCorrelation.spearman(neg(A), neg(B)), 1e-7);
  }

  private static float[] neg(float[] x) {
    float[] neg = new float[x.length];

    for (int i = 0; i < x.length; i++) {
      neg[i] = -x[i];
    }

    return neg;
  }

}
