package edu.unc.genomics;

import java.nio.file.Files;
import java.nio.file.Path;

import net.sf.picard.reference.FastaSequenceFile;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * @author timpalpant
 *
 */
public class FastaSequenceFileConverter implements IStringConverter<FastaSequenceFile> {

	@Override
	public FastaSequenceFile convert(String value) {
		PathConverter converter = new PathConverter();
		Path p = converter.convert(value);
		
		if (!Files.isReadable(p)) {
			throw new ParameterException("Cannot find/read input FASTA file " + value);
		}
		
		return new FastaSequenceFile(p.toFile(), true);
	}

}
