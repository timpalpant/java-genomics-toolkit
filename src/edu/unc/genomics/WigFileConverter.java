package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Path;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

/**
 * @author timpalpant
 *
 */
public class WigFileConverter implements IStringConverter<WigFile> {

	@Override
	public WigFile convert(String value) throws ParameterException {
		PathConverter converter = new PathConverter();
		Path p = converter.convert(value);
		try {
			return WigFile.autodetect(p);
		} catch (WigFileException | IOException e) {
			throw new ParameterException("Error autodetecting and initializing BigWig/Wig file");
		}
	}

}
