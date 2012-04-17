package edu.unc.utils;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

public enum Correlation {
	PEARSON("pearson"),
	SPEARMAN("spearman");
	
	private String name;
	
	Correlation(final String name) {
		this.name = name;
	}
	
	public static Correlation fromName(final String name) {
		for (Correlation c : Correlation.values()) {
			if (c.getName().equalsIgnoreCase(name)) {
				return c;
			}
		}
		
		return null;
	}
	
	public double compute(double[] x, double[] y) {
		switch (this) {
		case PEARSON:
			PearsonsCorrelation c = new PearsonsCorrelation();
			return c.correlation(x, y);
		case SPEARMAN:
			SpearmansCorrelation s = new SpearmansCorrelation();
			return s.correlation(x, y);
		default:
			return 0;
		}
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
}
