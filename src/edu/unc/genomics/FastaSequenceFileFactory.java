package edu.unc.genomics;

import net.sf.picard.reference.FastaSequenceFile;

import com.beust.jcommander.IStringConverterFactory;

/**
 * @author timpalpant
 *
 */
public class FastaSequenceFileFactory implements IStringConverterFactory {
	/* (non-Javadoc)
	 * @see com.beust.jcommander.IStringConverterFactory#getConverter(java.lang.Class)
	 */
	@Override
	public Class<FastaSequenceFileConverter> getConverter(Class forType) {
		if (forType.equals(FastaSequenceFile.class)) {
			return FastaSequenceFileConverter.class;
		} else {
			return null;
		}
	}

}
