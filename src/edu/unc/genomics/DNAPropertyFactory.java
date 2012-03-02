package edu.unc.genomics;

import org.genomeview.dnaproperties.DNAProperty;

import com.beust.jcommander.IStringConverterFactory;

/**
 * @author timpalpant
 *
 */
public class DNAPropertyFactory implements IStringConverterFactory {
	/* (non-Javadoc)
	 * @see com.beust.jcommander.IStringConverterFactory#getConverter(java.lang.Class)
	 */
	@Override
	public Class<DNAProperty> getConverter(Class forType) {
		if (forType.equals(DNAProperty.class)) {
			return DNAProperty.class;
		} else {
			return null;
		}
	}

}
