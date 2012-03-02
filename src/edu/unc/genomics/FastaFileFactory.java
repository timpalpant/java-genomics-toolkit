package edu.unc.genomics;

import com.beust.jcommander.IStringConverterFactory;

import edu.unc.genomics.io.FastaFile;

/**
 * @author timpalpant
 *
 */
public class FastaFileFactory implements IStringConverterFactory {
	/* (non-Javadoc)
	 * @see com.beust.jcommander.IStringConverterFactory#getConverter(java.lang.Class)
	 */
	@Override
	public Class<FastaFileConverter> getConverter(Class forType) {
		if (forType.equals(FastaFile.class)) {
			return FastaFileConverter.class;
		} else {
			return null;
		}
	}

}
