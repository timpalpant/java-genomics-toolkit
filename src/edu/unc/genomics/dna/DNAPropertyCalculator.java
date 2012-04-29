package edu.unc.genomics.dna;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import net.sf.picard.reference.FastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.util.StringUtil;

import org.apache.log4j.Logger;
import org.genomeview.dnaproperties.DNAProperty;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.utils.PicardUtils;

public class DNAPropertyCalculator extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(DNAPropertyCalculator.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (FASTA)", required = true)
	public FastaSequenceFile inputFile;
	@Parameter(names = {"-p", "--property"}, description = "DNA property to calculate", required = true)
	public String propertyName;
	@Parameter(names = {"-n", "--normalize"}, description = "Output normalized values")
	public boolean normalize = true;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wiggle)", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		DNAProperty property = DNAProperty.create(propertyName);
		if (property == null) {
			log.error("Unknown DNA property: "+propertyName);
			throw new CommandLineToolException("Unknown DNA property: "+propertyName);
		}
		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wiggle track header
			writer.write("track type=wiggle_0");
			writer.newLine();
			
			// Process each entry in the Fasta file in chunks
			// SAMSequenceDictionary dict = inputFile.getSequenceDictionary();
			//for (SAMSequenceRecord seq : dict.getSequences()) {
			//	log.debug("Processing FASTA entry "+seq.getSequenceName());
			ReferenceSequence seq;
			while ((seq = inputFile.nextSequence()) != null) {
				log.debug("Processing FASTA entry " + seq.getName());
				
				double[] values;
				if (normalize) {
					values = property.normalizedProfile(StringUtil.bytesToString(seq.getBases()));
				} else {
					values = property.profile(StringUtil.bytesToString(seq.getBases()));
				}
				
				// Write the chromosome header to output
				writer.write("fixedStep chrom="+seq.getName()+" start=1 step=1 span=1");
				writer.newLine();
				
				for (double value : values) {
					writer.write(Float.toString((float)value));
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
