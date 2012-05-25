package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileWriter;

/**
 * Condense a Wig file into an optimally compact format
 * @author timpalpant
 *
 */
public class Condense extends CommandLineTool {

	private static final Logger log = Logger.getLogger(Condense.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
		
	@Override
	public void run() throws IOException {
		try (WigFileReader reader = WigFileReader.autodetect(inputFile);
				 WigFileWriter writer = new WigFileWriter(outputFile, reader.getHeader())) {
			
		}
	}
	
	public static void main(String[] args) {
		new Condense().instanceMain(args);
	}

}
