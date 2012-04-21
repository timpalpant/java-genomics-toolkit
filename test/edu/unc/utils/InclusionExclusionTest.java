package edu.unc.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class InclusionExclusionTest {
	
	private static final float[] test = {0.2f, 0.8f, 0.7f};

	@Test
	public void testIndependent() {
		assertEquals(0.952, InclusionExclusion.independent(test), 1e-7);
	}
	
	@Test
	public void testIndependentSubset() {
		assertEquals(0.84, InclusionExclusion.independent(test, 0, 2), 1e-7);
	}

}
