package edu.unc.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class SortUtilsTest {

  private static final float[] A = { 0.1f, -4, 0.5f, 12, -3, -4.2f, 12.2f, 10.1f };
  private static int[] sortedIndicesA = { 5, 1, 4, 0, 2, 7, 3, 6 };
  private static int[] rankA = { 4, 2, 5, 7, 3, 1, 8, 6 };
  private static final float[] B = { 5, 4, 3, 2, 1, 0, -2.2f, 1.3f };
  private static int[] sortedIndicesB = { 6, 5, 4, 7, 3, 2, 1, 0 };
  private static int[] rankB = { 8, 7, 6, 5, 3, 2, 1, 4 };

  @Test
  public void testSortIndices() {
    assertArrayEquals(sortedIndicesA, SortUtils.sortIndices(A));
    assertArrayEquals(sortedIndicesB, SortUtils.sortIndices(B));
  }

  @Test
  public void testRank() {
    assertArrayEquals(rankA, SortUtils.rank(A));
    assertArrayEquals(rankB, SortUtils.rank(B));
  }

}
