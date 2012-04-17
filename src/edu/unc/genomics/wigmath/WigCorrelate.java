package edu.unc.genomics.wigmath;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.Correlation;

public class WigCorrelate extends CommandLineTool {

	private static final Logger log = Logger.getLogger(WigCorrelate.class);

	@Parameter(description = "Input files", required = true)
	public List<String> inputFiles = new ArrayList<String>();
	@Parameter(names = {"-w", "--window"}, description = "Window size (bp)")
	public Integer windowSize = 100;
	@Parameter(names = {"-t", "--type"}, description = "Correlation metric to use (pearson/spearman)")
	public String type = "pearson";
	@Parameter(names = {"-o", "--output"}, description = "Output file")
	public Path outputFile;
	
	private Correlation corr;
	private List<WigFile> wigs;
	private float[][] correlationMatrix;
	
	@Override
	public void run() throws IOException {
		if (inputFiles.size() < 2) {
			throw new CommandLineToolException("Cannot correlate < 2 input files.");
		}
		
		corr = Correlation.fromName(type);
		if (corr == null) {
			log.error("Unknown correlation metric: "+type);
			throw new CommandLineToolException("Unknown correlation metric: "+type+". Options are pearson, spearman");
		}
		
		log.debug("Initializing input files");
		for (String inputFile : inputFiles) {
			try {
				wigs.add(WigFile.autodetect(Paths.get(inputFile)));
			} catch (IOException | WigFileException e) {
				log.error("Error initializing input Wig file: " + inputFile);
				e.printStackTrace();
				throw new CommandLineToolException(e.getMessage());
			}
		}
		log.debug("Initialized " + wigs.size() + " input files");
		correlationMatrix = new float[wigs.size()][wigs.size()];
		
		// Compute the pairwise correlations between all files
		// only keeping two files in memory at any one time
		for (int i = 0; i < wigs.size(); i++) {
			// Get the data for file i
			
			for (int j = i+1; j < wigs.size(); j++) {
				// Get the data for file j
				
				// Correlate (i,j)
				//correlationMatrix[i][j] = corr.compute(binsi, binsj);
			}
		}
		
		// Write the correlation matrix to output
		StringBuilder output = new StringBuilder();
		// Header row
		for (int i = 0; i < wigs.size(); i++) {
			output.append("\t"+wigs.get(i).getPath().getFileName());
		}
		// Data rows
		for (int i = 0; i < wigs.size(); i++) {
			output.append("\n"+wigs.get(i).getPath().getFileName());
			for (int j = 0; j < wigs.size(); j++) {
				output.append("\t"+correlationMatrix[i][j]);
			}
		}
		
		if (outputFile != null) {
			log.debug("Writing to output file");
			try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
				writer.write(output.toString());
			}
		} else {
			System.out.println(output.toString());
		}
	}
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new WigCorrelate().instanceMain(args);
	}

}
