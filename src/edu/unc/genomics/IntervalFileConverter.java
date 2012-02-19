package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.io.IntervalFile;

/**
 * @author timpalpant
 *
 */
public class IntervalFileConverter implements IStringConverter<IntervalFile<? extends Interval>> {

	@Override
	public IntervalFile<? extends Interval> convert(String value) throws ParameterException {
		PathConverter converter = new PathConverter();
		Path p = converter.convert(value);
		
		if (!Files.isReadable(p)) {
			throw new ParameterException("Cannot find/read input interval file " + value);
		}
		
		try {
			return IntervalFile.autodetect(p);
		} catch (IOException e) {
			throw new ParameterException("IOException while attempting to autodetect interval file type");
		}
	}

}
