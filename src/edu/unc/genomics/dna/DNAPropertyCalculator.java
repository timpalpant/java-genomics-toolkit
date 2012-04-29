package edu.unc.genomics.dna;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import net.sf.picard.reference.FastaSequenceFile;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.util.StringUtil;

import org.apache.log4j.Logger;
import org.genomeview.dnaproperties.DNAProperty;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;

public class DNAPropertyCalculator extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(DNAPropertyCalculator.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (FASTA)", required = true)
	public Path inputFile;
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
		
		// TODO Generate the FASTA index if it is not already present (?)
		IndexedFastaSequenceFile fasta = null;
		try {
			fasta = new IndexedFastaSequenceFile(inputFile.toFile());
		} catch (FileNotFoundException e) {
			log.error("Could not find FASTA index. You must first index your FASTA file with 'samtools faidx'");
			e.printStackTrace();
			throw new CommandLineToolException(e.getMessage());
		}
		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wiggle track header
			writer.write("track type=wiggle_0");
			writer.newLine();
			
			// Process each entry in the FASTA file in chunks
			SAMSequenceDictionary dict = fasta.getSequenceDictionary();
			for (SAMSequenceRecord contig : dict.getSequences()) {
				log.debug("Processing FASTA entry "+contig.getSequenceName());
				// Write the contig header to output
				writer.write("fixedStep chrom="+contig.getSequenceName()+" start=1 step=1 span=1");
				writer.newLine();
				
				long start = 1;
				while (start <= contig.getSequenceLength()) {
					long stop = start + DEFAULT_CHUNK_SIZE - 1;
					ReferenceSequence seq = fasta.getSubsequenceAt(contig.getSequenceName(), start, stop);
					
					double[] values;
					if (normalize) {
						values = property.normalizedProfile(StringUtil.bytesToString(seq.getBases()));
					} else {
						values = property.profile(StringUtil.bytesToString(seq.getBases()));
					}
					
					for (double value : values) {
						writer.write(Float.toString((float)value));
						writer.newLine();
					}
					
					start = stop + 1;
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
