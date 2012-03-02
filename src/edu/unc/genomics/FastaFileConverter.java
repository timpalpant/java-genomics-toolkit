package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.io.FastaFile;

/**
 * @author timpalpant
 *
 */
public class FastaFileConverter implements IStringConverter<FastaFile> {

	@Override
	public FastaFile convert(String value) {
		PathConverter converter = new PathConverter();
		Path p = converter.convert(value);
		
		if (!Files.isReadable(p)) {
			throw new ParameterException("Cannot find/read input FASTA file " + value);
		}
		
		try {
			return new FastaFile(p);
		} catch (IOException e) {
			throw new ParameterException("IOException while attempting to open FASTA file: " + p);
		}
	}

}
