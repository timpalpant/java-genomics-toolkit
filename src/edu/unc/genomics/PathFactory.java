/**
 * 
 */
package edu.unc.genomics;

import java.nio.file.Path;

import com.beust.jcommander.IStringConverterFactory;

/**
 * @author timpalpant
 *
 */
public class PathFactory implements IStringConverterFactory {
  /*
   * (non-Javadoc)
   * 
   * @see
   * com.beust.jcommander.IStringConverterFactory#getConverter(java.lang.Class)
   */
  @Override
  public Class<PathConverter> getConverter(Class forType) {
    if (forType.equals(Path.class)) {
      return PathConverter.class;
    } else {
      return null;
    }
  }

}
