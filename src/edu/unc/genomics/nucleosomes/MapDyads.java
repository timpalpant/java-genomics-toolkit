package edu.unc.genomics.nucleosomes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.zip.DataFormatException;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.Assembly;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ValuedInterval;
import edu.unc.genomics.io.IntervalFile;
import edu.unc.genomics.io.IntervalFileSnifferException;

public class MapDyads {
	
	public static final int DEFAULT_CHUNK_SIZE = 500_000;
	
	private static final Logger log = Logger.getLogger(MapDyads.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads)", required = true)
	public String inputFile;
	@Parameter(names = {"-l", "--length"}, description = "Mononucleosome length (default: read length)")
	public int length;
	@Parameter(names = {"-a", "--assembly"}, description = "Genome assembly", required = true)
	public Assembly assembly;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wig)", required = true)
	public String outputFile;
		
	public void run() throws IOException, IntervalFileSnifferException, DataFormatException {		
		log.debug("Initializing input file");
		IntervalFile<? extends Interval> reads = IntervalFile.autodetect(Paths.get(inputFile));
		
		log.debug("Initializing output file");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
		TrackHeader header = new TrackHeader("wiggle_0");
		header.setName("Mapped Dyads: " + Paths.get(inputFile).getFileName());
		header.setDescription("Mapped Dyads: " + Paths.get(inputFile).getFileName());
		writer.write(header.toString());
		writer.newLine();
		
		for (String chr : assembly) {
			writer.write("fixedStep chrom="+chr+" start=1 step=1 span=1");
			
			
		}
	}
	
	public static void main(String[] args) throws IOException, IntervalFileSnifferException, DataFormatException {
		MapDyads a = new MapDyads();
		JCommander jc = new JCommander(a);
		jc.setProgramName(MapDyads.class.getSimpleName());
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			jc.usage();
			System.exit(-1);
		}
		
		a.run();
	}
}