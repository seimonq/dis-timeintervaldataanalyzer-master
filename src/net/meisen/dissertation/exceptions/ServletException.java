package net.meisen.dissertation.exceptions;

/**
 * Exception thrown when an error with the {@code Servlet} occurred.
 * 
 * @author pmeisen
 * 
 */
public class ServletException extends RuntimeException {
	private static final long serialVersionUID = -8755155188546569563L;

	/**
	 * Creates an exception which should been thrown whenever there is no other
	 * reason for the exception, i.e. the exception is the root.
	 * 
	 * @param message
	 *            the message of the exception
	 */
	public ServletException(final String message) {
		super(message);
	}

	/**
	 * Creates an exception which should been thrown whenever another
	 * <code>Throwable</code> is the reason for this.
	 * 
	 * @param message
	 *            the message of the exception
	 * @param t
	 *            the reason for the exception
	 */
	public ServletException(final String message, final Throwable t) {
		super(message, t);
	}
}