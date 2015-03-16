package edu.unc.genomics;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.beust.jcommander.IStringConverter;

/**
 * @author timpalpant
 *
 */
public class PathConverter implements IStringConverter<Path> {

  @Override
  public Path convert(String value) {
    return Paths.get(value);
  }

}
