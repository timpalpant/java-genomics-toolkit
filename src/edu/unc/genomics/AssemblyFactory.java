package edu.unc.genomics;

import com.beust.jcommander.IStringConverterFactory;

/**
 * @author timpalpant
 *
 */
public class AssemblyFactory implements IStringConverterFactory {

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.beust.jcommander.IStringConverterFactory#getConverter(java.lang.Class)
   */
  @Override
  public Class<AssemblyConverter> getConverter(Class forType) {
    if (forType.equals(Assembly.class)) {
      return AssemblyConverter.class;
    } else {
      return null;
    }
  }

}
