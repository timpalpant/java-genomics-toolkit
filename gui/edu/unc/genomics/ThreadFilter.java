package edu.unc.genomics;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A Log4j filter that filters for messages from a single thread
 * @author timpalpant
 *
 */
public class ThreadFilter extends Filter {
	
	private final String threadName;
	
	public ThreadFilter(String threadName) {
		this.threadName = threadName;
	}

	@Override
	public int decide(LoggingEvent e) {
		if(e.getThreadName().equalsIgnoreCase(threadName)) {
			return Filter.DENY;
		}
		
		return Filter.NEUTRAL;
	}

	/**
	 * @return the threadName
	 */
	public String getThreadName() {
		return threadName;
	}

}
