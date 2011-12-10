package edu.unc.genomics.visualization;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.BedEntry;
import edu.unc.genomics.io.BedFile;
import edu.unc.genomics.io.IntervalFileSnifferException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

public class MatrixAligner {

	private static final Logger log = Logger.getLogger(MatrixAligner.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (Wig)", required = true)
	public String inputFile;
	@Parameter(names = {"-l", "--loci"}, description = "Loci file (Bed)", required = true)
	public String lociFile;
	@Parameter(names = {"-m", "--max"}, description = "Truncate width (base pairs)")
	public int maxWidth = -1;
	@Parameter(names = {"-o", "--output"}, description = "Output file (matrix2png format)", required = true)
	public String outputFile;
	
	private WigFile wig;
	private List<BedEntry> loci = new ArrayList<BedEntry>();
	
	public void run() throws IOException, WigFileException, IntervalFileSnifferException {
		log.debug("Initializing input Wig file");
		wig = WigFile.autodetect(Paths.get(inputFile));
		
		log.debug("Loading alignment intervals");
		BedFile bed = new BedFile(Paths.get(lociFile));
		for (BedEntry entry : bed) {
			loci.add(entry);
		}
		bed.close();
		
		// Compute the matrix dimensions
		int leftMax = Integer.MIN_VALUE;
		int rightMax = Integer.MIN_VALUE;
		for (BedEntry entry : loci) {
			int left = Math.abs(entry.getValue().intValue()-entry.getStart());
			int right = Math.abs(entry.getValue().intValue()-entry.getStop());
			if (left > leftMax) {
				leftMax = left;
			}
			if (right > rightMax) {
				rightMax = right;
			}
		}
		
		int m = loci.size();
		int n = leftMax + rightMax + 1;
		int alignmentPoint = leftMax;
		log.info("Intervals aligned into: " + m+"x"+n + " matrix");
		log.info("Alignment point: " + alignmentPoint);
		
		int leftBound = 0;
		int rightBound = n-1;
		if (maxWidth != -1 && maxWidth < n) {
			log.info("Truncated to: " + m+"x"+maxWidth);
			int leftAlignDistance = alignmentPoint;
			int rightAlignDistance = n - alignmentPoint - 1;
			int halfMax = maxWidth / 2;
			
			if (halfMax < leftAlignDistance && halfMax < rightAlignDistance) {
				leftBound = alignmentPoint - halfMax;
				rightBound = alignmentPoint + halfMax;
			} else {
				if (leftAlignDistance <= rightAlignDistance) {
					rightBound = maxWidth;
				} else {
					leftBound = n - maxWidth;
				}
			}
		}
		
		
		log.debug("Initializing output file");
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile), Charset.defaultCharset());
		writer.write("ID");
		for (int i = leftBound-alignmentPoint; i <= rightBound-alignmentPoint; i++) {
			writer.write("\t"+i);
		}
		writer.newLine();
		
		log.debug("Iterating over all intervals");
		int count = 0;
		int skipped = 0;
		String[] row = new String[n];
		for (BedEntry entry : loci) {
			Iterator<WigItem> result = null;
			try {
				result = wig.query(entry);
			} catch (WigFileException e) {
				skipped++;
				continue;
			}
			
			float[] data = WigFile.flattenData(result, entry.getStart(), entry.getStop());
			// Reverse if on the crick strand
			if (entry.isCrick()) {
				ArrayUtils.reverse(data);
			}
			
			// Position the data in the matrix
			// Locus alignment point (entry value) should be positioned over the matrix alignment point
			int n1 = alignmentPoint - Math.abs(entry.getValue().intValue()-entry.getStart());
			int n2 = alignmentPoint + Math.abs(entry.getValue().intValue()-entry.getStop());
			assert data.length == n2-n1+1;
			
			Arrays.fill(row, "-");
			for (int i = 0; i < data.length; i++) {
				if (!Float.isNaN(data[i])) {
					row[n1+i] = String.valueOf(data[i]);
				}
			}
			
			// Write to output
			String id = ((entry.getId() == null) ? entry.getId() : "Row "+(count++));
			writer.write(id);
			for (int i = leftBound; i <= rightBound; i++) {
				writer.write("\t"+row[i]);
			}
			writer.newLine();
		}
		
		wig.close();
		writer.close();
		log.info(count + " intervals processed");
		log.info(skipped + " intervals skipped");
	}
	
	public static void main(String[] args) throws IOException, WigFileException, IntervalFileSnifferException {
		MatrixAligner application = new MatrixAligner();
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
