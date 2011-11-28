package edu.unc.genomics.wigmath;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.ngs.Autocorrelation;

public class WigSummary {

	private static final Logger log = Logger.getLogger(Autocorrelation.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public String inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file")
	public String outputFile;
	
	private WigFile wig;
	
	public void run() throws IOException, WigFileException {		
		log.debug("Initializing input Wig file");
		wig = WigFile.autodetect(Paths.get(inputFile));
		
		String summary = wig.toString();
		
		if (outputFile != null) {
			log.debug("Writing to output file");
			BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
			writer.write(summary);
			writer.close();
		} else {
			System.out.println(summary);
		}
	}
	
	public static void main(String[] args) throws IOException, WigFileException {
		WigSummary application = new WigSummary();
		JCommander jc = new JCommander(application);
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			jc.usage();
			System.exit(-1);
		}
		
		application.run();
	}

}
