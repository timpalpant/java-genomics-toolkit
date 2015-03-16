package edu.unc.genomics.visualization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.ReadablePathValidator;

/**
 * Remove the first column and row (headers) from a matrix in matrix2png format
 * so that the output is purely numerical for easy import into Matlab
 * 
 * @author timpalpant
 *
 */
public class StripMatrix extends CommandLineTool {

  @Parameter(names = { "-i", "--input" }, description = "Input file (matrix2png format)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-o", "--output" }, description = "Output file (tabular)", required = true)
  public Path outputFile;

  public void run() throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(inputFile, Charset.defaultCharset());
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      // Skip the first (header) line
      String line = reader.readLine();
      while ((line = reader.readLine()) != null) {
        String[] row = line.split("\t");
        for (int i = 1; i < row.length; i++) {
          String cell = row[i];
          if (cell.equalsIgnoreCase("-")) {
            writer.write("NaN");
          } else {
            writer.write(cell);
          }

          if (i < row.length - 1) {
            writer.write("\t");
          }
        }
        writer.newLine();
      }
    }
  }

  public static void main(String[] args) {
    new StripMatrix().instanceMain(args);
  }
}