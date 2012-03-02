package edu.unc.genomics;

import org.genomeview.dnaproperties.DNAProperty;

import com.beust.jcommander.IStringConverter;

/**
 * @author timpalpant
 *
 */
public class DNAPropertyConverter implements IStringConverter<DNAProperty> {
	
	@Override
	public DNAProperty convert(String value) {
		return DNAProperty.create(value);
	}

}
