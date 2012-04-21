package edu.unc.utils;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class FloatCorrelationTest {
	
	private static final float[] A = {0.1f, -4, 0.5f, 12, -3, -4.2f};
	private static final float[] B = {5, 4, 3, 2, 1, 0};

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testPearson() {
		assertEquals(1.0, FloatCorrelation.pearson(A, A), 1e-7);
		assertEquals(1.0, FloatCorrelation.pearson(B, B), 1e-7);
		assertEquals(0.061252558623857, FloatCorrelation.pearson(A, B), 1e-7);
	}

	@Test
	public void testSpearman() {
		assertEquals(1.0, FloatCorrelation.spearman(A, A), 1e-7);
		assertEquals(1.0, FloatCorrelation.spearman(B, B), 1e-7);
		assertEquals(0.314285714285714, FloatCorrelation.spearman(A, B), 1e-7);
	}

}
