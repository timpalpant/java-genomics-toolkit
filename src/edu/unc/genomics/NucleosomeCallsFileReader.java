package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Path;

import net.sf.samtools.TabixWriter;
import net.sf.samtools.TabixWriter.Conf;

import edu.unc.genomics.IntervalFactory;
import edu.unc.genomics.io.TextIntervalFileReader;

/**
 * Read nucleosome calls files
 * 
 * @author timpalpant
 *
 */
public class NucleosomeCallsFileReader extends TextIntervalFileReader<NucleosomeCall> {

  public NucleosomeCallsFileReader(Path p) throws IOException {
    super(p, new NucleosomeCallFactory());
  }

  public static class NucleosomeCallFactory implements IntervalFactory<NucleosomeCall> {

    public static final TabixWriter.Conf NUCLEOSOME_CALL_CONF = new TabixWriter.Conf(0, 1, 2, 3, '#', 0);

    @Override
    public NucleosomeCall parse(String line) {
      return NucleosomeCall.parse(line);
    }

    @Override
    public Conf tabixConf() {
      return NUCLEOSOME_CALL_CONF;
    }

  }

}
