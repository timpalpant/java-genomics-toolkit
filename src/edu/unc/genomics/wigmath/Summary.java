package edu.unc.genomics.wigmath;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.ngs.Autocorrelation;

/**
 * Output a summary of a (Big)Wig file with information about the chromosomes,
 * contigs, and statistics about the data.
 * 
 * @author timpalpant
 *
 */
public class Summary extends CommandLineTool {

  private static final Logger log = Logger.getLogger(Autocorrelation.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-o", "--output" }, description = "Output file")
  public Path outputFile;

  public void run() throws IOException {
    try (WigFileReader reader = WigFileReader.autodetect(inputFile)) {
      String summary = reader.toString();

      if (outputFile != null) {
        log.debug("Writing to output file");
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
          writer.write(summary);
        }
      } else {
        System.out.println(summary);
      }
    }
  }

  public static void main(String[] args) throws IOException, WigFileException {
    new Summary().instanceMain(args);
  }

}
