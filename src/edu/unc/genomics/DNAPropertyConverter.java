package edu.unc.genomics;

import org.genomeview.dnaproperties.DNAProperty;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * @author timpalpant
 *
 */
public class DNAPropertyConverter implements IStringConverter<DNAProperty> {

	private static final DNAProperty[] properties = DNAProperty.values();
	
	@Override
	public DNAProperty convert(String value) {
		for (DNAProperty p : properties) {
			if (p.toString().equalsIgnoreCase(value)) {
				return p;
			}
		}
		
		throw new ParameterException("Unknown or unavailable DNA property: " + value);
	}

}
