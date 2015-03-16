package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileWriter;

/**
 * Extracts the values from a (Big)Wig file for a given interval
 * 
 * @author timpalpant
 *
 */
public class ExtractRegion extends CommandLineTool {

  private static final Logger log = Logger.getLogger(ExtractRegion.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (BigWig/Wig)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-c", "--chr" }, description = "Chromosome", required = true)
  public String chr;
  @Parameter(names = { "-s", "--start" }, description = "Start base pair")
  public int start = -1;
  @Parameter(names = { "-e", "--stop" }, description = "Stop base pair")
  public int stop = -1;
  @Parameter(names = { "--chunk" }, description = "Size to chunk each chromosome into when processing (bp)")
  public int chunkSize = DEFAULT_CHUNK_SIZE;
  @Parameter(names = { "-f", "--fixedstep" }, description = "Force fixedStep output")
  public boolean fixedStep = false;
  @Parameter(names = { "-o", "--output" }, description = "Output file (wig)", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    try (WigFileReader reader = WigFileReader.autodetect(inputFile)) {
      if (!reader.includes(chr)) {
        throw new CommandLineToolException("Wig file does not contain chromosome " + chr);
      }
      if (start == -1) {
        start = reader.getChrStart(chr);
      }
      if (stop == -1) {
        stop = reader.getChrStop(chr);
      }
      Interval region = new Interval(chr, start, stop);
      if (!reader.includes(region)) {
        int chrStart = reader.getChrStart(chr);
        int chrStop = reader.getChrStop(chr);
        throw new CommandLineToolException("Wig file does not contain data for region " + region
            + ". Wig file has data for " + chr + ":" + chrStart + "-" + chrStop);
      }

      try (WigFileWriter writer = new WigFileWriter(outputFile, reader.getHeader())) {
        try {
          log.debug("Loading data for region " + region);
          Contig contig = reader.query(region);
          log.debug("Writing data to output file");
          if (fixedStep) {
            writer.writeFixedStepContig(contig);
          } else {
            writer.write(contig);
          }

        } catch (WigFileException e) {
          throw new CommandLineToolException(e);
        }
      }
    }
  }

  public static void main(String[] args) {
    new ExtractRegion().instanceMain(args);
  }
}