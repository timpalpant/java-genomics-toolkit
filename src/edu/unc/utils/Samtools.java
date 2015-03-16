package edu.unc.utils;

import java.nio.file.Path;

import org.apache.log4j.Logger;

import net.sf.picard.reference.IndexedFastaSequenceFile;

/**
 * Helper methods for calling the samtools executable externally Note: If at all
 * possible, these should be avoided since they require the user to have
 * samtools installed and available in the PATH
 * 
 * It is preferred to use functionality in SAM-JDK / Picard
 * 
 * @author timpalpant
 *
 */
public class Samtools {

  private static final Logger log = Logger.getLogger(Samtools.class);

  /**
   * Index a FASTA file with 'samtools faidx'
   * 
   * @param p
   *          the FASTA file to index
   * @throws Exception
   *           if the index is not created successfully
   */
  public static void indexFasta(Path p) throws Exception {
    log.debug("Attempting to generate FASTA index by calling 'samtools faidx'");

    try {
      Process proc = new ProcessBuilder("samtools", "faidx", p.toString()).start();
      proc.waitFor();
    } catch (Exception e) {
      log.error("Error attempting to call 'samtools faidx'. Is samtools available in the PATH?");
    } finally {
      if (!IndexedFastaSequenceFile.canCreateIndexedFastaReader(p.toFile())) {
        log.error("Could not create FASTA index for file " + p);
        throw new Exception("Could not create FASTA index for file " + p);
      }
    }
  }
}
