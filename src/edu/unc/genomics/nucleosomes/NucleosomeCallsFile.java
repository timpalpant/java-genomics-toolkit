/**
 * 
 */
package edu.unc.genomics.nucleosomes;

import java.io.IOException;
import java.nio.file.Path;

import edu.unc.genomics.IntervalFactory;
import edu.unc.genomics.io.TextIntervalFile;

/**
 * @author timpalpant
 *
 */
public class NucleosomeCallsFile extends TextIntervalFile<NucleosomeCall> {

	public NucleosomeCallsFile(Path p) throws IOException {
		super(p, new NucleosomeCallFactory());
	}
	
	public static class NucleosomeCallFactory implements IntervalFactory<NucleosomeCall> {
		
		@Override
		public NucleosomeCall parse(String line) {
			return NucleosomeCall.parse(line);
		}

	}

}
