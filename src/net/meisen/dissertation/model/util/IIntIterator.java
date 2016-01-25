package net.meisen.dissertation.model.util;

/**
 * Iterator used to iterate over integers.
 * 
 * @author pmeisen
 * 
 */
public interface IIntIterator {

	/**
	 * Is there more?
	 * 
	 * @return {@code true}, if there is more, {@code false} otherwise
	 */
	public boolean hasNext();

	/**
	 * Return the next integer.
	 * 
	 * @return the integer
	 */
	public int next();

}