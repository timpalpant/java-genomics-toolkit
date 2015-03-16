/**
 * 
 */
package edu.unc.genomics;

import java.nio.file.Files;
import java.nio.file.Path;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author timpalpant
 *
 */
public class ReadablePathValidator implements IParameterValidator {

  /*
   * (non-Javadoc)
   * 
   * @see com.beust.jcommander.IParameterValidator#validate(java.lang.String,
   * java.lang.String)
   */
  @Override
  public void validate(String name, String value) throws ParameterException {
    PathConverter converter = new PathConverter();
    Path p = converter.convert(value);
    if (!Files.isReadable(p)) {
      throw new ParameterException("Parameter " + name + " should be a readable file");
    }
  }

}
