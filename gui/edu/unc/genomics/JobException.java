package edu.unc.genomics;

/**
 * @author timpalpant
 *
 */
public class JobException extends Exception {

	private static final long serialVersionUID = -831504993593959450L;

	/**
	 * 
	 */
	public JobException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public JobException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public JobException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JobException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public JobException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
