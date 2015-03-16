package edu.unc.utils;

import net.sf.samtools.util.SequenceUtil;

/**
 * Helper methods for working with sequence data
 * 
 * @author timpalpant
 *
 */
public class SequenceUtils {
  /**
   * Search for the next index of a subsequence in a larger sequence, allowing
   * mismatches
   * 
   * @param bases
   *          the sequence to search in
   * @param nmer
   *          the nmer to search for
   * @param allowedMismatches
   *          the number of mismatches allowed
   * @param fromIndex
   *          the index to start searching at
   * @return the index of the next match of nmer in bases, or -1 if no matches
   *         are found
   */
  public static int indexOf(byte[] bases, byte[] nmer, int allowedMismatches, int fromIndex) {
    for (int i = fromIndex; i < bases.length - nmer.length; i++) {
      int mismatches = 0;
      for (int j = 0; j < nmer.length; j++) {
        if (!SequenceUtil.basesEqual(bases[i + j], nmer[j])) {
          if (++mismatches > allowedMismatches) {
            break;
          }
        }
      }

      // If we found one at this position, return the index
      if (mismatches <= allowedMismatches) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Search for the next index of a subsequence in a larger sequence, allowing
   * mismatches
   * 
   * @param bases
   *          the sequence to search in
   * @param nmer
   *          the nmer to search for
   * @param allowedMismatches
   *          the number of mismatches to allow
   * @return the index of the next match of nmer in bases, or -1 if no matches
   *         are found
   */
  public static int indexOf(byte[] bases, byte[] nmer, int allowedMismatches) {
    return indexOf(bases, nmer, allowedMismatches, 0);
  }

  /**
   * Search for the next index of a subsequence in a larger sequence, with no
   * mismatches
   * 
   * @param bases
   *          the sequence to search in
   * @param nmer
   *          the nmer to search for
   * @return the index of the next match of nmer in bases, or -1 if no matches
   *         are found
   */
  public static int indexOf(byte[] bases, byte[] nmer) {
    return indexOf(bases, nmer, 0);
  }
}
