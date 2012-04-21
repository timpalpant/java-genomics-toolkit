package edu.unc.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class SortUtilsTest {

	private static final float[] A = {0.1f, -4, 0.5f, 12, -3, -4.2f};
	private static int[] expectedA = {5, 1, 4, 0, 2, 3};
	private static final float[] B = {5, 4, 3, 2, 1, 0};
	private static int[] expectedB = {5, 4, 3, 2, 1, 0};

	@Test
	public void test() {
		assertArrayEquals(expectedA, SortUtils.rank(A));
		assertArrayEquals(expectedB, SortUtils.rank(B));
	}

}
