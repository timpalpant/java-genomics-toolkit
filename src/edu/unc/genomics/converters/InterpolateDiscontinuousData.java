package edu.unc.genomics.converters;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.igv.bbfile.WigItem;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.io.WigFile;
import edu.unc.genomics.io.WigFileException;
import edu.unc.genomics.wigmath.WigMathTool;

public class InterpolateDiscontinuousData extends WigMathTool {

	private static final Logger log = Logger.getLogger(InterpolateDiscontinuousData.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (Wig)", required = true)
	public WigFile inputFile;
	@Parameter(names = {"-t", "--type"}, description = "Interpolant (nearest/linear/cubic)")
	public String type = "linear";
	@Parameter(names = {"-m", "--max"}, description = "Maximum span of missing values to interpolate (bp)")
	public int max = 1000;

	private Interpolant interp;
	
	@Override
	public void setup() {
		// Validate that the interpolant is a known type
		interp = Interpolant.fromName(type);
		if (interp == null) {
			log.error("Unknown interpolation method: "+type);
			throw new CommandLineToolException("Unknown interpolation method: "+type+". Options are nearest, linear, pchip");
		} else {
			log.debug("Using interpolant: "+type);
		}
		
		if (max > DEFAULT_CHUNK_SIZE) {
			log.warn("Allowable span of missing values exceeds processing chunk size");
		}
		
		inputs.add(inputFile);
	}
	
	@Override
	public float[] compute(String chr, int start, int stop) throws IOException, WigFileException {
		Iterator<WigItem> data = inputFile.query(chr, start, stop);
		float[] result = WigFile.flattenData(data, start, stop);
		
		// Special case: if the first or last value in this chunk is missing, 
		// then we'll need to look further up/downstream to do the interpolation
		// Find the first and last base pairs that have data
		int first = 0;
		while (Float.isNaN(result[first]) && (++first < result.length));
		int last = result.length-1;
		while (Float.isNaN(result[last]) && (--last > first));
		
		// If the entire chunk is missing, skip it
		if (first == result.length) {
			log.warn("Skipping entire chunk "+chr+":"+start+"-"+stop+" that is missing values");
			return result;
		}
		
		// If we are missing values at the beginning, but not exceeding the allowable span
		if (first > 0 && first < max) {	
			// Need to look upstream
			int upstream = max - first;
			int queryStart = Math.max(start-upstream, inputFile.getChrStart(chr));
			int queryStop = start-1;
			if (queryStart >= queryStop) {
				log.debug("Cannot interpolate missing values at the start of chromosome "+chr);
			} else {
				Iterator<WigItem> upstreamData = inputFile.query(chr, queryStart, queryStop);
				float[] upstreamResult = WigFile.flattenData(upstreamData, queryStart, queryStop);
				int j = upstreamResult.length-1;
				while (Float.isNaN(upstreamResult[j]) && (--j >= 0));
				// If we found a value within range, do the interpolation
				if (j >= 0 && !Float.isNaN(upstreamResult[j])) {
					int x0 = -j;
					int x1 = first;
					float y0 = upstreamResult[j];
					float y1 = result[first];
					doInterpolation(result, x0, x1, y0, y1);
				}
			}
		}
	
		// If we are missing values at the end, but not exceeding the allowable span
		if (last < result.length-1 && last >= result.length-max) {
			// Need to look downstream
			int downstream = max - (result.length-last);
			int queryStart = stop+1;
			int queryStop = Math.min(stop+downstream, inputFile.getChrStop(chr));
			if (queryStart >= queryStop) {
				log.debug("Cannot interpolate missing values at the end of chromosome "+chr);
			} else {
				Iterator<WigItem> downstreamData = inputFile.query(chr, queryStart, queryStop);
				float[] downstreamResult = WigFile.flattenData(downstreamData, queryStart, queryStop);
				int j = 0;
				while (Float.isNaN(downstreamResult[j]) && (++j < downstreamResult.length));
				// If we found a value within range, do the interpolation
				if (j < downstreamResult.length && !Float.isNaN(downstreamResult[j])) {
					int x0 = last;
					int x1 = result.length+j;
					float y0 = result[last];
					float y1 = downstreamResult[j];
					doInterpolation(result, x0, x1, y0, y1);
				}
			}
		}
		
		// Find and process interior intervals of missing values that need interpolation
		for (int i = first; i < last; i++) {
			if (Float.isNaN(result[i])) {
				int x0 = i-1;
				while (Float.isNaN(result[++i]));
				int x1 = i - 1;
				if (x1-x0 <= max) {
					doInterpolation(result, x0, x1, result[x0], result[x1]);
				} else {
					log.debug("Skipping interval "+chr+":"+(start+x0)+"-"+(start+x1)+" (exceeds maximum span)");
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Interpolate values in an array
	 * @param x array of values
	 * @param x0 start index of the interpolation
	 * @param x1 end index of the interpolation
	 * @param y0 start value of the interpolation
	 * @param y1 end value of the interpolation
	 */
	protected void doInterpolation(float[] x, int x0, int x1, float y0, float y1) {
		switch(interp) {
		case NEAREST:
			int center = (x0 + x1) / 2;
			for (int i = x0+1; i <= center; i++) {
				if (i > 0 && i < x.length) {
					x[i] = y0;
				}
			}
			for (int i = center+1; i < y0; i++) {
				if (i > 0 && i < x.length) {
					x[i] = y1;
				}
			}
			break;
		case LINEAR:
			float m = (y1 - y0) / (x1 - x0);
			for (int i = x0+1; i < x1; i++) {
				if (i > 0 && i < x.length) {
					x[i] = y0 + m*(i-x0);
				}
			}
			break;
		case CUBIC:
			int l = x1 - x0;
			float a = (y1-y0) / (l*l);
			float b = 2*(y0-y1) / (l*l*l);
			for (int i = x0+1; i < x1; i++) {
				if (i > 0 && i < x.length) {
					x[i] = y0 + a*(i-x0)*(i-x0) + b*(i-x0)*(i-x0)*(i-x1);
				}
			}
			break;
		}
	}
	
	/**
	 * @param args
	 * @throws WigFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, WigFileException {
		new InterpolateDiscontinuousData().instanceMain(args);
	}

	/**
	 * An enumeration of known interpolation methods
	 * @author timpalpant
	 *
	 */
	public enum Interpolant {
		NEAREST("nearest"),
		LINEAR("linear"),
		CUBIC("cubic");
		
		private String name;
		
		Interpolant(final String name) {
			this.name = name;
		}
		
		public static Interpolant fromName(final String name) {
			for (Interpolant i : Interpolant.values()) {
				if (i.getName().equalsIgnoreCase(name)) {
					return i;
				}
			}
			
			return null;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
	}
}
