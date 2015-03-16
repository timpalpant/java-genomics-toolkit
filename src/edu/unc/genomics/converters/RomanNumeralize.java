package edu.unc.genomics.converters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.utils.RomanNumeral;

/**
 * Convert instances of "chr12" to "chrXII" in a text file, etc.
 * 
 * @author timpalpant
 *
 */
public class RomanNumeralize extends CommandLineTool {

  private static final Logger log = Logger.getLogger(RomanNumeralize.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-o", "--output" }, description = "Output file", required = true)
  public Path outputFile;

  /**
   * Pattern for finding "chr12" tokens (will find "chr1" through "chr99")
   */
  Pattern p = Pattern.compile("chr[\\d]{1,2}");

  @Override
  public void run() throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(inputFile, Charset.defaultCharset());
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      log.debug("Copying input to output and replacing with Roman Numerals");
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher m = p.matcher(line);
        StringBuffer converted = new StringBuffer(line.length());
        while (m.find()) {
          String chrNum = line.substring(m.start() + 3, m.end());
          int arabic = Integer.parseInt(chrNum);
          String roman = RomanNumeral.int2roman(arabic);
          m.appendReplacement(converted, "chr" + roman);
        }
        m.appendTail(converted);

        writer.write(converted.toString());
        writer.newLine();
      }
    }
  }

  public static void main(String[] args) {
    new RomanNumeralize().instanceMain(args);
  }

}
