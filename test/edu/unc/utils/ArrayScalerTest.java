package edu.unc.utils;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ArrayScalerTest {

  private static final double[] TEST_DATA = { 0, 0.479425539, 0.841470985, 0.997494987, 0.909297427, 0.598472144,
      0.141120008, -0.350783228, -0.756802495, -0.977530118 };
  private ArrayScaler scaler;

  @Before
  public void setUp() throws Exception {
    scaler = new ArrayScaler(TEST_DATA);
  }

  @Test
  public void testGetScaled() {
    double[] scaled5 = scaler.getScaled(5);
    assertEquals(scaled5.length, 5);

    double[] scaled15 = scaler.getScaled(15);
    assertEquals(scaled15.length, 15);

    double[] scaled20 = scaler.getScaled(20);
    assertEquals(scaled20.length, 20);
  }

}
