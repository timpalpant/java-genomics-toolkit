package edu.unc.utils;

import static org.junit.Assert.*;

import org.junit.Test;

public class RomanNumeralTest {

  @Test
  public void test() {
    assertEquals("I", RomanNumeral.int2roman(1));
    assertEquals("IX", RomanNumeral.int2roman(9));
    assertEquals("XI", RomanNumeral.int2roman(11));
    assertEquals("XIV", RomanNumeral.int2roman(14));
    assertEquals("XVI", RomanNumeral.int2roman(16));
  }

}
