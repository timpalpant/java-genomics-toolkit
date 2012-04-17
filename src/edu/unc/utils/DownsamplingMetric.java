package edu.unc.utils;

public enum DownsamplingMetric {
	MEAN("mean"),
	MIN("min"),
	MAX("max");
	
	private String name;
	
	DownsamplingMetric(final String name) {
		this.name = name;
	}
	
	public static DownsamplingMetric fromName(final String name) {
		for (DownsamplingMetric dsm : DownsamplingMetric.values()) {
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