package edu.unc.utils;

/**
 * An enumeration of the statistics that we know how to compute on Wig data
 * These correspond to the statistics that are built into the UCSC BigWig tools
 * 
 * @author timpalpant
 *
 */
public enum WigStatistic {
  COVERAGE("coverage"), TOTAL("total"), MEAN("mean"), MIN("min"), MAX("max");

  private String name;

  WigStatistic(final String name) {
    this.name = name;
  }

  public static WigStatistic fromName(final String name) {
    for (WigStatistic dsm : WigStatistic.values()) {
      if (dsm.getName().equalsIgnoreCase(name)) {
        return dsm;
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