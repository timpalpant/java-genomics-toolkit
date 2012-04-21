package edu.unc.utils;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class FloatHistogramTest {
	
	private FloatHistogram hist;
	private static final int[] expected = {0, 0, 2, 0, 0, 1, 0, 1, 1, 0};

	@Before
	public void setUp() throws Exception {
		hist = new FloatHistogram(10, 0, 1);
		hist.addValue(0.54);
		hist.addValue(0.21);
		hist.addValue(0.2);
		hist.addValue(0.88);
		hist.addValue(0.72);
	}

	@Test
	public void testGetHistogram() {
		assertArrayEquals(expected, hist.getHistogram());
	}

	@Test
	public void testGetBinSize() {
		assertEquals(0.1, hist.getBinSize(), 1e-8);
	}

	@Test
	public void testReset() {
		hist.reset();
		assertArrayEquals(new int[10], hist.getHistogram());
	}

}
