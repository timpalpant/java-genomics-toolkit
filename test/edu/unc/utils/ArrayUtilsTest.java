package edu.unc.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArrayUtilsTest {

  private static final float[] TEST = { 1.0f, 5.0f, -4.0f, 6.0f, 0.0f };
  private static final float[] TIE = { 1.0f, 5.0f, -4.0f, 5.0f, 0.0f };

  @Test
  public void testMaxIndex() {
    int maxIndex = ArrayUtils.maxIndex(TEST);
    assertEquals(3, maxIndex);
  }

  @Test
  public void testMaxIndexTie() {
    int maxIndex = ArrayUtils.maxIndex(TIE);
    assertEquals(1, maxIndex);
  }

}
