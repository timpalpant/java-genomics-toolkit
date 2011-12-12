package edu.unc.genomics;

import com.beust.jcommander.IStringConverterFactory;

import edu.unc.genomics.io.IntervalFile;

/**
 * @author timpalpant
 *
 */
public class IntervalFileFactory implements IStringConverterFactory {

	/* (non-Javadoc)
	 * @see com.beust.jcommander.IStringConverterFactory#getConverter(java.lang.Class)
	 */
	@Override
	public Class<IntervalFileConverter> getConverter(Class forType) {
		if (forType.equals(IntervalFile.class)) {
			return IntervalFileConverter.class;
		} else {
			return null;
		}
	}

}
