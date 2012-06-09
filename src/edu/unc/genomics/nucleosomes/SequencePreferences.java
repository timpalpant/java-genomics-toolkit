package edu.unc.genomics.nucleosomes;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import net.sf.picard.reference.FastaSequenceIndex;
import net.sf.picard.reference.FastaSequenceIndexEntry;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.util.SequenceUtil;
import net.sf.samtools.util.StringUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.genomeview.dnaproperties.DNAProperty;

import com.beust.jcommander.Parameter;

import edu.ucsc.genome.TrackHeader;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.Contig;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileWriter;
import edu.unc.utils.Samtools;

/**
 * This tool gets the sequence underlying a list of reads or intervals,
 * and counts the frequency of each di-, tri-, etc. nucleotide at each position
 * from the center of the read.
 * 
 * @author timpalpant
 *
 */
public class SequencePreferences extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(SequencePreferences.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (reads/intervals)", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-n", "--order"}, description = "Order of frequencies to compute")
	public int order = 2;
	@Parameter(names = {"-w", "--width"}, description = "Distance from fragment center to count frequencies")
	public int width = 200;
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		if (!IndexedFastaSequenceFile.canCreateIndexedFastaReader(inputFile.toFile())) {
			try {
				Samtools.indexFasta(inputFile);
			} catch (Exception e) {
				log.error("Error indexing FASTA file with samtools. Is samtools available in the PATH?");
				throw new CommandLineToolException("Error indexing FASTA file with samtools");
			}
		}
		
		// Open the FASTA file and its index
		IndexedFastaSequenceFile fasta = new IndexedFastaSequenceFile(inputFile.toFile());
		Path indexFile = inputFile.resolveSibling(inputFile.getFileName()+".fai");
		FastaSequenceIndex faidx = new FastaSequenceIndex(indexFile.toFile());
		
		// Process each entry in the FASTA file in chunks
		try (IntervalFileReader<? extends Interval> reader = IntervalFileReader.autodetect(inputFile)) {
			for (FastaSequenceIndexEntry contig : faidx) {
				log.debug("Processing FASTA entry "+contig.getContig()+" (length = "+contig.getSize()+")");
				int start = 1;
				while (start <= contig.getSize()) {
					int stop = start+DEFAULT_CHUNK_SIZE-1;
					int paddedStart = Math.max(start-width, 1);
					int paddedStop = (int) Math.min(stop+width, contig.getSize());
					Interval chunk = new Interval(contig.getContig(), start, stop);
					log.debug("Processing chunk "+chunk);
					ReferenceSequence seq = fasta.getSubsequenceAt(contig.getContig(), paddedStart, paddedStop);
					String bases = StringUtil.bytesToString(seq.getBases());
					
					// Loop over the reads/intervals for this chunk
					Iterator<? extends Interval> it = reader.query(chunk);
					while (it.hasNext()) {
						Interval interval = it.next();
						int fragmentStart = interval.center() - width;
						int fragmentStop = interval.center() + width;
						if (fragmentStart >= chunk.getStart() && fragmentStop <= chunk.getStop()) {
							for (int i = fragmentStart; i <= fragmentStop; i++) {
								
							}
						}
					}
					
					start = stop + 1;
				}
			}
		}
		
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile, Charset.defaultCharset()))) {
			
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new SequencePreferences().instanceMain(args);
	}

}
