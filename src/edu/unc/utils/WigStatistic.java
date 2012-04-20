package edu.unc.utils;

public enum WigStatistic {
	MEAN("mean"),
	MIN("min"),
	MAX("max");
	
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