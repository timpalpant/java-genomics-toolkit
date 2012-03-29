package edu.unc.genomics.converters;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.GeneTrackEntry;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.io.GeneTrackFile;

public class GeneTrackToBedGraph extends CommandLineTool {

	private static final Logger log = Logger.getLogger(GeneTrackToBedGraph.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (GeneTrack format)", required = true)
	public Path gtFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (BedGraph)", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		log.debug("Initializing input GeneTrack file");
		GeneTrackFile gt = new GeneTrackFile(gtFile);
		
		log.debug("Initializing output file");
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			for (GeneTrackEntry entry : gt) {
				writer.write(entry.toBedGraph());
				writer.newLine();
			}
		} finally {
			gt.close();
		}
	}
	
	public static void main(String[] args) {
		new GeneTrackToBedGraph().instanceMain(args);
	}

}
