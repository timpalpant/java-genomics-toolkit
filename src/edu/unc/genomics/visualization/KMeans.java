package edu.unc.genomics.visualization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.clustering.Cluster;
import org.apache.commons.math3.stat.clustering.KMeansPlusPlusClusterer;
import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.KMeansRow;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileSnifferException;
import edu.unc.genomics.io.WigFileException;

/**
 * Cluster a heatmap matrix with k-means
 * 
 * @author timpalpant
 *
 */
public class KMeans extends CommandLineTool {

  private static final Logger log = Logger.getLogger(KMeans.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (matrix2png format)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-k", "--clusters" }, description = "Number of clusters")
  public int k = 10;
  @Parameter(names = { "-1", "--min" }, description = "Minimum column to use for clustering")
  public int minCol = 1;
  @Parameter(names = { "-2", "--max" }, description = "Maximum column to use for clustering")
  public Integer maxCol;
  @Parameter(names = { "-o", "--output" }, description = "Output file (clustered matrix2png format)", required = true)
  public Path outputFile;

  private Map<String, String> rows = new HashMap<String, String>();
  private List<KMeansRow> data = new ArrayList<KMeansRow>();

  @Override
  public void run() throws IOException {
    log.debug("Loading data from the input matrix");
    String headerLine = "";
    try (BufferedReader reader = Files.newBufferedReader(inputFile, Charset.defaultCharset())) {
      // Header line
      int lineNum = 1;
      headerLine = reader.readLine();
      int numColsInMatrix = StringUtils.countMatches(headerLine, "\t");

      // Validate the range info
      if (maxCol != null && maxCol != -1) {
        if (maxCol > numColsInMatrix) {
          throw new RuntimeException("Invalid range of data specified for clustering (" + maxCol + " > "
              + numColsInMatrix + ")");
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
        String[] row = line.substring(delim + 1).split("\t");
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
    }

    // Perform the clustering
    log.debug("Clustering the data");
    Random rng = new Random();
    KMeansPlusPlusClusterer<KMeansRow> clusterer = new KMeansPlusPlusClusterer<KMeansRow>(rng);
    List<Cluster<KMeansRow>> clusters = clusterer.cluster(data, k, 50);

    // Write to output
    log.debug("Writing clustered data to output file");
    try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      writer.write(headerLine);
      writer.newLine();
      int n = 1;
      int count = 1;
      for (Cluster<KMeansRow> cluster : clusters) {
        int numRowsInCluster = cluster.getPoints().size();
        int stop = count + numRowsInCluster - 1;
        log.info("Cluster " + (n++) + ": rows " + count + "-" + stop);
        count = stop + 1;
        for (KMeansRow row : cluster.getPoints()) {
          writer.write(rows.get(row.getId()));
          writer.newLine();
        }
      }
    }
  }

  public static void main(String[] args) throws IOException, WigFileException, IntervalFileSnifferException {
    new KMeans().instanceMain(args);
  }

}
