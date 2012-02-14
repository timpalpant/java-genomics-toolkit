package edu.unc.genomics;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author timpalpant
 *
 */
public class PositiveIntegerValidator implements IParameterValidator {

	/* (non-Javadoc)
	 * @see com.beust.jcommander.IParameterValidator#validate(java.lang.String, java.lang.String)
	 */
	@Override
	public void validate(String name, String value) throws ParameterException {		
		int n = Integer.parseInt(value);
		if (n <= 0) {
			throw new ParameterException("Parameter "+name+" must be > 0 (was "+value+")");
		}
	}

}
