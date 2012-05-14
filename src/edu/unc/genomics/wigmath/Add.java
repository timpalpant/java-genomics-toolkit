package edu.unc.genomics.wigmath;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;

/**
 * This tool will add all values in the specified Wig files base pair by base pair.
 * @author timpalpant
 *
 */
public class Add extends WigMathTool {

	private static final Logger log = Logger.getLogger(Add.class);

	@Parameter(description = "Input files", required = true)
	public List<String> inputFiles = new ArrayList<String>();

	@Override
	public void setup() {
		if (inputFiles.size() < 2) {
			throw new CommandLineToolException("No reason to add < 2 files.");
		}
		
		log.debug("Initializing input files");
		for (String inputFile : inputFiles) {
			try {
				addInputFile(WigFile.autodetect(Paths.get(inputFile)));
			} catch (IOException | WigFileException e) {
				log.error("Error initializing input Wig file: " + inputFile);
				e.printStackTrace();
				throw new CommandLineToolException(e.getMessage());
			}
		}
		log.debug("Initialized " + inputs.size() + " input files");
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		int length = stop - start + 1;
		float[] sum = new float[length];
		
		for (WigFile wig : inputs) {
			Iterator<WigItem> data = wig.query(chr, start, stop);
			while (data.hasNext()) {
				WigItem item = data.next();
				for (int i = item.getStartBase(); i <= item.getEndBase(); i++) {
					if (i-start >= 0 && i-start < sum.length) {
						sum[i-start] += item.getWigValue();
					}
				}
			}
		}
		
		return sum;
	}
	
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new Add().instanceMain(args);
	}

}
