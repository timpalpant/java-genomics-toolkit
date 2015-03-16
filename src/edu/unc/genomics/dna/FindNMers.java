package edu.unc.genomics.dna;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import net.sf.picard.reference.FastaSequenceIndex;
import net.sf.picard.reference.FastaSequenceIndexEntry;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.util.SequenceUtil;
import net.sf.samtools.util.StringUtil;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.BedFileWriter;
import edu.unc.utils.Samtools;
import edu.unc.utils.SequenceUtils;

/**
 * This tool finds NMers in a genomic DNA sequence and creates a new Bed file
 * with the location of matches.
 * 
 * @author timpalpant
 *
 */
public class FindNMers extends CommandLineTool {

  private static final Logger log = Logger.getLogger(FindNMers.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (FASTA)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-n", "--nmer" }, description = "NMer to search for", required = true)
  public String nmerStr;
  @Parameter(names = { "-m", "--mismatches" }, description = "Number of allowed mismatches")
  public int allowedMismatches = 0;
  @Parameter(names = { "-r", "--rc" }, description = "Search reverse complement as well")
  public boolean rc = false;
  @Parameter(names = { "-o", "--output" }, description = "Output file (Wiggle)", required = true)
  public Path outputFile;

  private byte[] nmer;
  private byte[] rcNmer;

  @Override
  public void run() throws IOException {
    log.debug("Searching for NMer " + nmerStr);
    nmer = StringUtil.stringToBytes(nmerStr);
    rcNmer = Arrays.copyOf(nmer, nmer.length);
    SequenceUtil.reverseComplement(rcNmer);
    if (rc) {
      log.debug("Searching for reverse complement " + StringUtil.bytesToString(rcNmer));
    }

    if (!IndexedFastaSequenceFile.canCreateIndexedFastaReader(inputFile.toFile())) {
      try {
        Samtools.indexFasta(inputFile);
      } catch (Exception e) {
        log.error("Error indexing FASTA file with samtools. Is samtools available in the PATH?");
        throw new CommandLineToolException("Error indexing FASTA file with samtools");
      }
    }

    // Open the FASTA file and its index
    IndexedFastaSequenceFile fasta = new IndexedFastaSequenceFile(inputFile.toFile());
    Path indexFile = inputFile.resolveSibling(inputFile.getFileName() + ".fai");
    FastaSequenceIndex faidx = new FastaSequenceIndex(indexFile.toFile());

    try (BedFileWriter<Interval> writer = new BedFileWriter<>(outputFile)) {
      // Process each entry in the FASTA file in chunks
      for (FastaSequenceIndexEntry contig : faidx) {
        log.debug("Processing FASTA entry " + contig.getContig() + " (length = " + contig.getSize() + ")");
        int start = 1;
        while (start <= contig.getSize()) {
          int stop = (int) Math.min(start + DEFAULT_CHUNK_SIZE - 1, contig.getSize());
          log.debug("Processing chunk " + contig.getContig() + ":" + start + "-" + stop);
          ReferenceSequence seq = fasta.getSubsequenceAt(contig.getContig(), start, stop);
          byte[] bases = seq.getBases();

          // Search for forward matches
          int pos = 0;
          while ((pos = SequenceUtils.indexOf(bases, nmer, allowedMismatches, pos)) != -1) {
            Interval match = new Interval(contig.getContig(), start + pos, start + pos + nmer.length);
            writer.write(match);
            pos++;
          }

          // Search for reverse-complement matches
          if (rc) {
            pos = 0;
            while ((pos = SequenceUtils.indexOf(bases, rcNmer, allowedMismatches, pos)) != -1) {
              Interval match = new Interval(contig.getContig(), start + pos + rcNmer.length, start + pos);
              writer.write(match);
              pos++;
            }
          }

          start = stop + 1;
        }
      }
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    new FindNMers().instanceMain(args);
  }

}
