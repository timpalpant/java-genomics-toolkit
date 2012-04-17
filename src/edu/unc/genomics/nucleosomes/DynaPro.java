package edu.unc.genomics.nucleosomes;

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

public class DynaPro extends CommandLineTool {

	private static final Logger log = Logger.getLogger(DynaPro.class);

	@Parameter(names = {"-i", "--input"}, description = "Energy landscape", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-n", "--size"}, description = "Nucleosome size (bp)")
	public int nucleosomeSize = 147;
	@Parameter(names = {"-m", "--mean"}, description = "Shift energy landscape to have mean")
	public Double newMean;
	@Parameter(names = {"-v", "--variance"}, description = "Scale energy landscape to variance")
	public Double newVar;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wig)", required = true)
	public Path outputFile;
	
	Float shift, scale;
	
	@Override
	public void run() throws IOException {
		if (newMean != null) {
			log.debug("Shifting mean of energy landscape from "+inputFile.mean()+" to "+newMean);
			shift = (float) (newMean - inputFile.mean());
		}
		
		if (newVar != null) {
			log.debug("Rescaling variance of energy landscape from "+Math.pow(inputFile.stdev(),2)+" to "+newVar);
			scale = (float) Math.sqrt(newVar / Math.pow(inputFile.stdev(), 2));
		}
		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wig header
			writer.write("track type=wiggle_0");
			writer.newLine();
			
			for (String chr : inputFile.chromosomes()) {
				log.debug("Processing chromosome " + chr);
				int start = inputFile.getChrStart(chr);
				int stop = inputFile.getChrStop(chr);
				
				// Process the chromosome
				Iterator<WigItem> data = null;
				try {
					data = inputFile.query(chr, start, stop);
				} catch (WigFileException e) {
					log.fatal("Error querying Wig file for data from chromosome "+chr);
					e.printStackTrace();
					throw new CommandLineToolException("Error querying Wig file for data from chromosome "+chr);
				}
				float[] energy = WigFile.flattenData(data, start, stop, 0);
				
				// Shift and rescale the energy landscape if specified
				if (shift != null) {
					for (int i = 0; i < energy.length; i++) {
						energy[i] += shift;
					}
				}
				if (scale != null) {
					for (int i = 0; i < energy.length; i++) {
						energy[i] *= scale;
					}
				}
				
				// Compute the probabilities
				float[] forward = new float[energy.length];
				for (int i = nucleosomeSize; i < energy.length; i++) {
					double factor = 1 + Math.exp(forward[i-nucleosomeSize] - forward[i-1] - energy[i-nucleosomeSize]);
					forward[i] = (float) (forward[i-1] + Math.log(factor));
				}

				float[] backward = new float[energy.length];
				for (int i = energy.length-nucleosomeSize-1; i > 0; i--) {
					double factor = 1 + Math.exp(backward[i+nucleosomeSize] - backward[i+1] - energy[i-1]);
					backward[i] = (float) (backward[i+1] + Math.log(factor));
				}
				
				float[] p = new float[energy.length];
				for (int i = 0; i < energy.length-nucleosomeSize; i++) {
					p[i] = (float) Math.exp(forward[i] - energy[i] + backward[i+nucleosomeSize] - backward[1]);
				}

				// Write the chromosome to output
				writer.write("fixedStep chrom="+chr+" start="+start+" step=1 span=1");
				writer.newLine();
				for (int i = 0; i < p.length; i++) {
					writer.write(Float.toString(p[i]));
					writer.newLine();
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
		new DynaPro().instanceMain(args);
	}

}
