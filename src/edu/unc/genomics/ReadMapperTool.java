package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.io.WigFileWriter;

/**
 * Abstract base class for writing programs that take reads and produce counts
 * in Wig files
 * 
 * ReadMapperTool takes the input reads file, finds the intersecting set of
 * chromosomes with the specified Assembly, and then iterates through the reads
 * in a chunk-by-chunk fashion, calling compute() on each chunk
 * 
 * The compute method must return the counts for that chunk (one value for each
 * base pair) which will then be written into a new output Wig file.
 * 
 * @author timpalpant
 *
 */
public abstract class ReadMapperTool extends CommandLineTool {

  private static final Logger log = Logger.getLogger(ReadMapperTool.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path intervalFile;
  @Parameter(names = { "-a", "--assembly" }, description = "Genome assembly", required = true)
  public Assembly assembly;
  @Parameter(names = { "-c", "--chunk" }, description = "Size to chunk each chromosome into when processing (bp)")
  public int chunkSize = DEFAULT_CHUNK_SIZE;
  @Parameter(names = { "-f", "--fixedstep" }, description = "Force fixedStep output")
  public boolean fixedStep = false;
  @Parameter(names = { "-o", "--output" }, description = "Output file", required = true)
  public Path outputFile;
  @Parameter(names = { "-b", "--split" }, description = "Write each chromosome into its own output file")
  public boolean split = false;

  /**
   * Do the computation on a chunk and return the results Must return
   * chunk.length() values (one for every base pair in chunk)
   * 
   * @param chunk
   *          the interval to process
   * @return the results of the computation for this chunk
   * @throws IOException
   * @throws WigFileException
   */
  public abstract float[] compute(IntervalFileReader<? extends Interval> reader, Interval chunk) throws IOException;

  @Override
  public final void run() throws IOException {
    log.debug("Processing reads and writing result to disk");
    TrackHeader header = TrackHeader.newWiggle();
    header.setName("Processed " + intervalFile.getFileName());
    header.setDescription("Processed " + intervalFile.getFileName());
    try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(intervalFile)) {
      if (split) {
        for (String chr : reader.chromosomes()) {
          // Construct a filename for this chromosome from the given output
          // filename
          String outputFileStr = outputFile.getFileName().toString();
          String base = outputFileStr.substring(0, outputFileStr.lastIndexOf('.'));
          String ext = outputFileStr.substring(outputFileStr.lastIndexOf('.'));
          try (WigFileWriter writer = new WigFileWriter(outputFile.resolveSibling(base + "." + chr + ext), header)) {
            processChromosome(reader, writer, chr);
          }
        }
      } else {
        try (WigFileWriter writer = new WigFileWriter(outputFile, header)) {
          for (String chr : reader.chromosomes()) {
            processChromosome(reader, writer, chr);
          }
        }
      }
    }
  }

  private void processChromosome(IntervalFileReader<? extends Interval> reader, WigFileWriter writer, String chr)
      throws IOException {
    if (!assembly.includes(chr)) {
      log.info("Skipping " + chr + " not in assembly " + assembly);
      return;
    }

    log.debug("Processing chromosome " + chr);
    int chunkStart = 1;
    while (chunkStart < assembly.getChrLength(chr)) {
      int chunkStop = Math.min(chunkStart + chunkSize - 1, assembly.getChrLength(chr));
      Interval chunk = new Interval(chr, chunkStart, chunkStop);
      log.debug("Processing chunk " + chunk);
      float[] result = compute(reader, chunk);

      // Verify that the computation returned the correct number of values for
      // the chunk
      if (result.length != chunk.length()) {
        log.error("Expected result length=" + chunk.length() + ", got=" + result.length);
        throw new CommandLineToolException("Result of mapping computation is not the expected length!");
      }

      // Write the count at each base pair to the output file
      if (fixedStep) {
        writer.writeFixedStepContig(new Contig(chunk, result));
      } else {
        writer.write(new Contig(chunk, result));
      }

      // Process the next chunk
      chunkStart = chunkStop + 1;
    }
  }
}
