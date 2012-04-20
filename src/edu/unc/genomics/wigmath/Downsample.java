package edu.unc.genomics.wigmath;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.WigStatistic;

public class Downsample extends CommandLineTool {

	private static final Logger log = Logger.getLogger(Downsample.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-w", "--window"}, description = "Window size (bp)")
	public int windowSize = 100;
	@Parameter(names = {"-m", "--metric"}, description = "Downsampling metric (mean/min/max)")
	public String metric = "mean";
	@Parameter(names = {"-o", "--output"}, description = "Output file", required = true)
	public Path outputFile;
		
	@Override
	public void run() throws IOException {
		WigStatistic dsm = WigStatistic.fromName(metric);
		if (dsm == null) {
			log.error("Unknown downsampling metric: "+metric);
			throw new CommandLineToolException("Unknown downsampling metric: "+metric+". Options are mean, min, max");
		} else {
			log.debug("Using downsampling metric: "+metric);
		}
		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wig header
			writer.write("track type=wiggle_0 name='Downsampled "+inputFile.getPath().getFileName()+"' description='Downsampled "+inputFile.getPath().getFileName()+"'");
			writer.newLine();
			
			for (String chr : inputFile.chromosomes()) {
				log.debug("Processing chromosome "+chr);
				int start = inputFile.getChrStart(chr);
				int stop = inputFile.getChrStop(chr);
				
				// Write the chromosome header to output
				writer.write("fixedStep chrom="+chr+" start="+start+" step="+windowSize+" span="+windowSize);
				writer.newLine();
				
				// Process the chromosome in chunks
				int bp = start;
				while (bp < stop) {
					int chunkStart = bp;
					int chunkStop = Math.min(bp+windowSize-1, stop);
					
					try {
						// Get the original data for this window from the Wig file
						Iterator<WigItem> result = inputFile.query(chr, chunkStart, chunkStop);
						// Do the downsampling
						float value = Float.NaN;
						switch (dsm) {
						case MEAN:
							value = WigFile.mean(result, chunkStart, chunkStop);
							break;
						case MIN:
							value = WigFile.min(result, chunkStart, chunkStop);
							break;
						case MAX:
							value = WigFile.max(result, chunkStart, chunkStop);
							break;
						}
						// Write the downsampled value to the output file
						writer.write(String.valueOf(value));
						writer.newLine();
					} catch (WigFileException e) {
						log.error("Error querying Wig file for data from interval "+chr+":"+chunkStart+"-"+chunkStop);
						e.printStackTrace();
						throw new CommandLineToolException("Error querying Wig file for data from interval "+chr+":"+chunkStart+"-"+chunkStop);
					}
					
					bp = chunkStop + 1;
				}
			}
		}
	}
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new Downsample().instanceMain(args);
	}

}
