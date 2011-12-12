package edu.unc.genomics;

import com.beust.jcommander.IStringConverterFactory;

import edu.unc.genomics.io.WigFile;

/**
 * @author timpalpant
 *
 */
public class WigFileFactory implements IStringConverterFactory {

	/* (non-Javadoc)
	 * @see com.beust.jcommander.IStringConverterFactory#getConverter(java.lang.Class)
	 */
	@Override
	public Class<WigFileConverter> getConverter(Class forType) {
		if (forType.equals(WigFile.class)) {
			return WigFileConverter.class;
		} else {
			return null;
		}
	}

}
