package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.Interval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.IntervalFileSnifferException;

public class RollingReadLength {
	
	private static final Logger log = Logger.getLogger(RollingReadLength.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads)", required = true)
	public String inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (read length histogram)", required = true)
	public String outputFile;
		
	public void run() throws IOException {		
		log.debug("Initializing input file");
		
	}
	
	public static void main(String[] args) throws IOException {
		RollingReadLength a = new RollingReadLength();
		JCommander jc = new JCommander(a);
		jc.setProgramName(RollingReadLength.class.getSimpleName());
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			jc.usage();
			System.exit(-1);
		}
		
		a.run();
	}
}