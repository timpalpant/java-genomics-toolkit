package edu.unc.genomics.converters;

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
import edu.unc.genomics.BedGraphEntry;
import edu.unc.genomics.BuiltInAssemblyLoader;
import edu.unc.genomics.io.BedGraphFile;

public class BedGraphToWig {

	private static final Logger log = Logger.getLogger(BedGraphToWig.class);
	public static final int DEFAULT_CHUNK_SIZE = 500_000;

	@Parameter(names = {"-i", "--input"}, description = "Input file (BedGraph)", required = true)
	public String inputFile;
	@Parameter(names = {"-a", "--assembly"}, description = "Genome assembly", required = true)
	public String genome;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wig)", required = true)
	public String outputFile;
	
	public void run() throws IOException, DataFormatException {		
		log.debug("Initializing input BedGraph file");
		BedGraphFile bedgraph = new BedGraphFile(Paths.get(inputFile));
		log.info(bedgraph.count() + " entries in input");
		
		log.debug("Initializing assembly");
		Assembly assembly = BuiltInAssemblyLoader.loadAssembly(genome);
		
		log.debug("Initializing output file");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
		TrackHeader header = new TrackHeader("wiggle_0");
		header.setName("Converted " + Paths.get(inputFile).getFileName());
		header.setDescription("Converted " + Paths.get(inputFile).getFileName());
		writer.write(header.toString());
		writer.newLine();
		
		for (String chr : assembly) {
			int start = 1;
			while (start < assembly.getChrLength(chr)) {
				int stop = start + DEFAULT_CHUNK_SIZE - 1;
				int length = stop - start + 1;
				int[] count = new int[length];
				float[] sum = new float[length];
				
				Iterator<BedGraphEntry> it = bedgraph.query(chr, start, stop);
				while (it.hasNext()) {
					BedGraphEntry entry = it.next();
					for (int i = entry.getStart(); i <= entry.getStop(); i++) {
						sum[i-start] += entry.getValue().floatValue();
						count[i-start]++;
					}
				}
				
				for (int i = 0; i < sum.length; i++) {
					if (count[i] == 0) {
						writer.write(String.valueOf(Float.NaN));
					} else {
						writer.write(String.valueOf(sum[i]/count[i]));
					}
					writer.newLine();
				}
				
				start = stop + 1;
			}
		}
	}
	
	public static void main(String[] args) throws IOException, DataFormatException {
		BedGraphToWig application = new BedGraphToWig();
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
