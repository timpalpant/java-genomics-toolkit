package edu.unc.utils;

/**
 * Rudimentary Class for doing Arabic Integer -> Roman Numeral conversion
 * 
 * @author timpalpant
 * @author Fred Swartz
 *
 */
public class RomanNumeral {

  final static RomanValue[] ROMAN_VALUE_TABLE = { new RomanValue(1000, "M"), new RomanValue(900, "CM"),
      new RomanValue(500, "D"), new RomanValue(400, "CD"), new RomanValue(100, "C"), new RomanValue(90, "XC"),
      new RomanValue(50, "L"), new RomanValue(40, "XL"), new RomanValue(10, "X"), new RomanValue(9, "IX"),
      new RomanValue(5, "V"), new RomanValue(4, "IV"), new RomanValue(1, "I") };

  /**
   * Convert an int to Roman numeral
   * 
   * @param n
   *          an integer between 1-3999
   * @return n as a Roman numeral
   */
  public static String int2roman(int n) {
    if (n >= 4000 || n < 1) {
      throw new NumberFormatException("Numbers must be in range 1-3999");
    }

    // ... Start with largest value, and work toward smallest.
    StringBuilder result = new StringBuilder(10);
    for (RomanValue equiv : ROMAN_VALUE_TABLE) {
      // ... Remove as many of this value as possible (maybe none).
      while (n >= equiv.intVal) {
        n -= equiv.intVal; // Subtract value.
        result.append(equiv.romVal); // Add roman equivalent.
      }
    }

    return result.toString();
  }

  private static class RomanValue {
    // ... No need to make this fields private because they are
    // used only in this private value class.
    int intVal; // Integer value.
    String romVal; // Equivalent roman numeral.

    RomanValue(int dec, String rom) {
      this.intVal = dec;
      this.romVal = rom;
    }
  }
}