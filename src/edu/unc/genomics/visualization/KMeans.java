package edu.unc.genomics.visualization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math.stat.clustering.Cluster;
import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;
import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.unc.genomics.io.IntervalFileSnifferException;
import edu.unc.genomics.io.WigFileException;

public class KMeans {

	private static final Logger log = Logger.getLogger(KMeans.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (matrix2png format)", required = true)
	public String inputFile;
	@Parameter(names = {"-k", "--clusters"}, description = "Number of clusters", required = true)
	public int k;
	@Parameter(names = {"-1", "--min"}, description = "Minimum column to use for clustering")
	public int minCol = 1;
	@Parameter(names = {"-2", "--max"}, description = "Maximum column to use for clustering")
	public int maxCol = -1;
	@Parameter(names = {"-o", "--output"}, description = "Output file (clustered matrix2png format)", required = true)
	public String outputFile;
	
	private Map<String, String> rows = new HashMap<String, String>();
	private List<KMeansRow> data = new ArrayList<KMeansRow>();
	
	public void run() throws IOException, WigFileException, IntervalFileSnifferException {
		log.debug("Loading data from the input matrix");
		Path input = Paths.get(inputFile);
		BufferedReader reader = Files.newBufferedReader(input, Charset.defaultCharset());
		
		// Header line
		int lineNum = 1;
		String headerLine = reader.readLine();
		int numColsInMatrix = StringUtils.countMatches(headerLine, "\t");
		
		// Validate the range info
		if (maxCol != -1) {
			if (maxCol > numColsInMatrix) {
				throw new RuntimeException("Invalid range of data specified for clustering");
			}
		} else {
			maxCol = numColsInMatrix;
		}
		
		// Loop over the rows and load the data
		String line;
		while ((line = reader.readLine()) != null) {
			lineNum++;
			if (StringUtils.countMatches(line, "\t") != numColsInMatrix) {
				throw new RuntimeException("Irregular input matrix does not have same number of columns on line " + lineNum);
			}
			
			int delim = line.indexOf('\t');
			String id = line.substring(0, delim);
			String[] row = line.substring(delim+1).split("\t");
			String[] subset = Arrays.copyOfRange(row, minCol, maxCol);
			float[] rowData = new float[subset.length];
			for (int i = 0; i < subset.length; i++) {
				try {
					rowData[i] = Float.parseFloat(subset[i]);
				} catch (NumberFormatException e) {
					rowData[i] = Float.NaN;
				}
			}
			data.add(new KMeansRow(id, rowData));
			rows.put(id, line);
		}
		
		// Perform the clustering
		log.debug("Clustering the data");
		Random rng = new Random();
		KMeansPlusPlusClusterer<KMeansRow> clusterer = new KMeansPlusPlusClusterer<KMeansRow>(rng);
		List<Cluster<KMeansRow>> clusters = clusterer.cluster(data, k, 50);
		
		// Write to output
		log.debug("Writing clustered data to output file");
		Path output = Paths.get(outputFile);
		BufferedWriter writer = Files.newBufferedWriter(output, Charset.defaultCharset());
		writer.write(headerLine);
		writer.newLine();
		int n = 1;
		int count = 1;
		for (Cluster<KMeansRow> cluster : clusters) {
			int numRowsInCluster = cluster.getPoints().size();
			int stop = count + numRowsInCluster - 1;
			log.info("Cluster "+(n++)+": rows "+count+"-"+stop);
			count = stop+1;
			for (KMeansRow row : cluster.getPoints()) {
				writer.write(rows.get(row.getId()));
				writer.newLine();
			}
		}
		writer.close();
	}
	
	public static void main(String[] args) throws IOException, WigFileException, IntervalFileSnifferException {
		KMeans application = new KMeans();
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
