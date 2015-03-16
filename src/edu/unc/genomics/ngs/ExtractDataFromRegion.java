package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Interval;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.io.WigFileReader;

/**
 * Extracts the values from a (Big)Wig file for a given interval
 * 
 * @author timpalpant
 *
 */
public class ExtractDataFromRegion extends CommandLineTool {

  private static final Logger log = Logger.getLogger(ExtractDataFromRegion.class);

  @Parameter(description = "Input files (BigWig/Wig)", required = true)
  public List<String> inputFiles = new ArrayList<String>();
  @Parameter(names = { "-c", "--chr" }, description = "Chromosome", required = true)
  public String chr;
  @Parameter(names = { "-s", "--start" }, description = "Start base pair", required = true)
  public int start;
  @Parameter(names = { "-e", "--stop" }, description = "Stop base pair", required = true)
  public int stop;
  @Parameter(names = { "-o", "--output" }, description = "Output file (tabular)", required = true)
  public Path outputFile;

  @Override
  public void run() throws IOException {
    // Query each wig file and get the data for the region
    log.debug("Loading data for region from " + inputFiles.size() + " wig files");
    Interval region = new Interval(chr, start, stop);
    float[][] data = new float[inputFiles.size()][region.length()];
    for (int i = 0; i < inputFiles.size(); i++) {
      try (WigFileReader reader = WigFileReader.autodetect(Paths.get(inputFiles.get(i)))) {
        data[i] = reader.query(region).getValues();
      } catch (WigFileException e) {
        throw new CommandLineToolException(e);
      }
    }

    // Write the data to output
    log.debug("Writing data to output file");
    try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      // Header line
      writer.write(chr);
      for (String inputFile : inputFiles) {
        writer.write("\t" + Paths.get(inputFile).getFileName());
      }
      writer.newLine();

      int dir = region.isWatson() ? 1 : -1;
      for (int i = 0; i < region.length(); i++) {
        int bp = start + dir * i;
        writer.write(String.valueOf(bp));

        for (int j = 0; j < data.length; j++) {
          writer.write("\t" + data[j][i]);
        }
        writer.newLine();
      }
    }
  }

  public static void main(String[] args) {
    new ExtractDataFromRegion().instanceMain(args);
  }
}