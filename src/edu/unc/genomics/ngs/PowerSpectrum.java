package edu.unc.genomics.ngs;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.io.WigFile;

public class PowerSpectrum extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(PowerSpectrum.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (Wig)", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (tabular)", required = true)
	public Path outputFile;
		
	public void run() throws IOException {		
		
		
	}
	
	public static void main(String[] args) {
		new PowerSpectrum().instanceMain(args);
	}
}