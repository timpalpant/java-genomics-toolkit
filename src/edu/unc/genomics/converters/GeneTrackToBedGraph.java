package edu.unc.genomics.converters;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.GeneTrackEntry;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.io.BedGraphFileWriter;
import edu.unc.genomics.io.GeneTrackFileReader;

/**
 * Convert a GeneTrack format file to BedGraph, adding the +/- strand values
 * 
 * @author timpalpant
 *
 */
public class GeneTrackToBedGraph extends CommandLineTool {

  private static final Logger log = Logger.getLogger(GeneTrackToBedGraph.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (GeneTrack format)", required = true)
  public Path gtFile;
  @Parameter(names = { "-o", "--output" }, description = "Output file (BedGraph)", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    log.debug("Initializing input/output files");
    try (GeneTrackFileReader gt = new GeneTrackFileReader(gtFile);
        BedGraphFileWriter<GeneTrackEntry> writer = new BedGraphFileWriter<>(outputFile)) {
      for (GeneTrackEntry entry : gt) {
        writer.write(entry);
      }
    }
  }

  public static void main(String[] args) {
    new GeneTrackToBedGraph().instanceMain(args);
  }

}
