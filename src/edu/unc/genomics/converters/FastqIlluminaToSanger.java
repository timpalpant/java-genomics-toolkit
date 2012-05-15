package edu.unc.genomics.converters;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import net.sf.picard.fastq.FastqReader;
import net.sf.picard.fastq.FastqRecord;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.ReadablePathValidator;

/**
 * Converts a FASTQ file with Illumina quality scores (Phred+64) to Sanger quality scores (Phred+33)
 * @author timpalpant
 *
 */
public class FastqIlluminaToSanger extends CommandLineTool {

	private static final Logger log = Logger.getLogger(FastqIlluminaToSanger.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (FASTQ, Illumina)", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-o", "--output"}, description = "Output file (FASTQ, Sanger)", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		FastqReader reader = new FastqReader(inputFile.toFile());
		
		int count = 0;
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			for (FastqRecord r : reader) {
				writer.write("@");
				writer.write(r.getReadHeader());
				writer.newLine();
				
				writer.write(r.getReadString());
				writer.newLine();
				
				writer.write("+");
				writer.write(r.getBaseQualityHeader());
				writer.newLine();
				
				// Convert the quality score to Sanger format
				char[] qual = r.getBaseQualityString().toCharArray();
				for (int i = 0; i < qual.length; i++) {
					qual[i] -= 31;
				}
				writer.write(qual);
				writer.newLine();
				
				count++;
			}
		}
		
		log.info("Processed "+count+" reads");
	}
	
	public static void main(String[] args) {
		new FastqIlluminaToSanger().instanceMain(args);
	}

}
