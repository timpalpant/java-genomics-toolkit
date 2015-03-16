package edu.unc.genomics.ngs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;
import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.Interval;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.genomics.io.IntervalFileReader;
import edu.unc.genomics.io.WigFileReader;
import edu.unc.genomics.io.WigFileException;
import edu.unc.utils.FFTUtils;

/**
 * Computes the normalized power spectrum of intervals of (Big)Wig data
 * 
 * @author timpalpant
 *
 */
public class PowerSpectrum extends CommandLineTool {

  private static final Logger log = Logger.getLogger(PowerSpectrum.class);

  @Parameter(names = { "-i", "--input" }, description = "Input file (Wig)", required = true, validateWith = ReadablePathValidator.class)
  public Path inputFile;
  @Parameter(names = { "-l", "--loci" }, description = "Genomic loci (Bed format)", required = true, validateWith = ReadablePathValidator.class)
  public Path lociFile;
  @Parameter(names = { "-m", "--max" }, description = "Only output this many frequencies")
  public int max = 40;
  @Parameter(names = { "-o", "--output" }, description = "Output file (tabular)", required = true)
  public Path outputFile;

  public void run() throws IOException {
    try (WigFileReader wig = WigFileReader.autodetect(inputFile);
        IntervalFileReader<? extends Interval> loci = IntervalFileReader.autodetect(lociFile);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
      // Write header
      writer.write("#chr\tlow\thigh\tid\talignment\tstrand\tPower Spectrum Values");
      writer.newLine();

      log.debug("Computing power spectrum for each window");
      int skipped = 0;
      for (Interval interval : loci) {
        float[] data;
        try {
          data = wig.query(interval).getValues();
        } catch (IOException | WigFileException e) {
          log.debug("Skipping interval: " + interval);
          skipped++;
          continue;
        }

        if (interval.length() > 1) {
          // Compute the power spectrum
          FloatFFT_1D fft = new FloatFFT_1D(data.length);
          fft.realForward(data);
          float[] ps = FFTUtils.abs2(data);
          // and normalize the power spectrum
          float sum = 0;
          for (int i = 1; i < ps.length; i++) {
            sum += ps[i];
          }
          for (int i = 1; i < ps.length; i++) {
            ps[i] /= sum;
          }

          writer.write(interval.toBed());
          for (int i = 1; i < Math.min(ps.length, max); i++) {
            writer.write("\t" + ps[i]);
          }
          writer.newLine();
        } else {
          skipped++;
          writer.write(interval.toBed());
          writer.newLine();
        }
      }

      log.info("Skipped " + skipped + " intervals");
    }
  }

  public static void main(String[] args) {
    new PowerSpectrum().instanceMain(args);
  }
}