package edu.unc.genomics.dna;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;
import org.genomeview.dnaproperties.DNAProperty;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.FastaEntry;
import edu.unc.genomics.io.FastaFile;

public class DNAPropertyCalculator extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(DNAPropertyCalculator.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (FASTA)", required = true)
	public FastaFile inputFile;
	@Parameter(names = {"-p", "--property"}, description = "DNA property to calculate", required = true)
	public String propertyName;
	@Parameter(names = {"-n", "--normalize"}, description = "Output normalized values")
	public boolean normalize = true;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wiggle)", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		DNAProperty property = DNAProperty.create(propertyName);
		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wiggle track header
			writer.write("track type=wiggle_0");
			writer.newLine();
			
			// Process each entry in the Fasta file
			// TODO Will break if entries in Fasta file are large
			for (FastaEntry f : inputFile) {
				log.debug("Processing FASTA entry "+f.getId());
				
				double[] values;
				if (normalize) {
					values = property.normalizedProfile(f.getSeq());
				} else {
					values = property.profile(f.getSeq());
				}
				
				// Write the chromosome header to output
				writer.write("fixedStep chrom="+f.getId()+" start=1 step=1 span=1");
				writer.newLine();
				
				for (double value : values) {
					writer.write(Double.toString(value));
					writer.newLine();
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DNAPropertyCalculator().instanceMain(args);
	}

}
