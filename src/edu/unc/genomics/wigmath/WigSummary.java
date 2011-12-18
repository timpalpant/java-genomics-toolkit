package edu.unc.genomics.wigmath;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.ngs.Autocorrelation;

public class WigSummary extends CommandLineTool {

	private static final Logger log = Logger.getLogger(Autocorrelation.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file")
	public Path outputFile;
		
	public void run() throws IOException {		
		String summary = inputFile.toString();
		
		if (outputFile != null) {
			log.debug("Writing to output file");
			try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
				writer.write(summary);
			}
		} else {
			System.out.println(summary);
		}
	}
	
	public static void main(String[] args) throws IOException, WigFileException {
		new WigSummary().instanceMain(args);
	}

}
