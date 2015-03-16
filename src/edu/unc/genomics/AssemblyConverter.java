package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.DataFormatException;

import org.apache.log4j.Logger;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * @author timpalpant
 *
 */
public class AssemblyConverter implements IStringConverter<Assembly> {

  private static final Logger log = Logger.getLogger(AssemblyConverter.class);

  public static final Path ASSEMBLIES_DIR = Paths.get("resources", "assemblies");

  @Override
  public Assembly convert(String value) throws ParameterException {
    // Look for the assembly in the resources/assemblies directory
    Path p = ASSEMBLIES_DIR.resolve(value + ".len");

    // If it does not exist in the assemblies directory, check if it is a path
    // to a file
    if (!Files.isReadable(p)) {
      PathConverter converter = new PathConverter();
      p = converter.convert(value);
      // If it does not exist, then throw an exception that the assembly cannot
      // be found
      if (!Files.isReadable(p)) {
        throw new ParameterException("Cannot find Assembly file: " + value);
      }
    }

    // Attempt to load the assembly from file
    try {
      return new Assembly(p);
    } catch (IOException | DataFormatException e) {
      log.error("Error loading Assembly from file: " + p);
      throw new ParameterException(e);
    }
  }

}
