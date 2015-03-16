package edu.unc.genomics;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;

/**
 * A command-line script
 * 
 * @author timpalpant
 *
 */
public abstract class CommandLineTool {

  /**
   * The default bite-size to use for applications that process files in chunks
   * TODO Read from a configuration file
   */
  public static final int DEFAULT_CHUNK_SIZE = 10_000_000;

  /**
   * Do the main computation of this tool
   * 
   * @throws IOException
   */
  public abstract void run() throws IOException;

  /**
   * Parse command-line arguments and run the tool Exit on parameter exceptions
   * 
   * @param args
   */
  public void instanceMain(String[] args) throws CommandLineToolException {
    // Initialize the command-line options parser
    JCommander jc = new JCommander(this);

    // Add factories for parsing Paths, Assemblies, IntervalFiles, and WigFiles
    jc.addConverterFactory(new PathFactory());
    jc.addConverterFactory(new AssemblyFactory());

    // Set the program name to be the class name
    String[] nameParts = getClass().getName().split("\\.");
    String shortName = StringUtils.join(Arrays.copyOfRange(nameParts, nameParts.length - 2, nameParts.length), '.');
    jc.setProgramName(shortName);

    try {
      jc.parse(args);
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      jc.usage();
      System.exit(-1);
    }

    ValidationStringency stringency = SAMFileReader.getDefaultValidationStringency();
    try {
      SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
      run();
    } catch (IOException e) {
      throw new CommandLineToolException("IO error while running", e);
    } finally {
      SAMFileReader.setDefaultValidationStringency(stringency);
    }
  }
}
