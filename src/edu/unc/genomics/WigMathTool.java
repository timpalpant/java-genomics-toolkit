package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.io.WigFileWriter;

/**
 * Abstract base class for writing programs to do computation on Wig files
 * Concrete subclasses must implement the compute method
 * 
 * The compute method must return the output values for that chunk (one value
 * for each base pair) which will then be written into a new output Wig file.
 * 
 * @author timpalpant
 * 
 */
public abstract class WigMathTool extends WigAnalysisTool {

  private static final Logger log = Logger.getLogger(WigMathTool.class);

  @Parameter(names = { "-f", "--fixedstep" }, description = "Force fixedStep output")
  public boolean fixedStep = false;
  @Parameter(names = { "-v", "--variablestep" }, description = "Force variableStep output")
  public boolean variableStep = false;
  @Parameter(names = { "-o", "--output" }, required = true, description = "Output Wig file")
  public Path outputFile;

  private WigFileWriter writer;

  /**
   * Setup the computation, and add all input Wig files
   */
  protected abstract void setup();

  /**
   * Do the computation on a chunk and return the results. Must return
   * chunk.length() values (one for every base pair in chunk)
   * 
   * @param chunk
   *          the interval to process
   * @return the results of the computation for this chunk
   * @throws IOException
   * @throws WigFileException
   */
  protected abstract float[] compute(Interval chunk) throws IOException, WigFileException;

  /**
   * Setup the computation. Should add all input Wig files with addInputFile()
   * during setup
   * 
   * @throws IOException
   */
  @Override
  protected final void prepare() {
    // Setup the input files
    setup();

    // Setup the output file
    try {
      writer = new WigFileWriter(outputFile, TrackHeader.newWiggle());
    } catch (IOException e) {
      throw new CommandLineToolException("Error initializing output file " + outputFile, e);
    }
  }

  /**
   * Shutdown the computation and do any cleanup / final processing
   * 
   * @throws IOException
   */
  @Override
  protected final void shutdown() throws IOException {
    writer.close();
    super.shutdown();
  }

  @Override
  public final void process(Interval chunk) throws IOException, WigFileException {
    float[] result = compute(chunk);

    // Verify that the computation returned the correct number of
    // values for the chunk
    if (result.length != chunk.length()) {
      log.error("Expected result length=" + chunk.length() + ", got=" + result.length);
      throw new CommandLineToolException("Result of Wig computation is not the expected length!");
    }

    // Write the result of the computation for this chunk to disk
    Contig outputContig = new Contig(chunk, result);
    if (fixedStep) {
      writer.writeFixedStepContig(outputContig);
    } else if (variableStep) {
      writer.writeVariableStepContig(outputContig);
    } else {
      writer.write(outputContig);
    }
  }
}
