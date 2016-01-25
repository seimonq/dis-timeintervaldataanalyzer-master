package net.kern.master.preaggregates;

/**
 * customized Exception for PreAggregate package
 * 
 * @author skern
 */
public class PreAggregateException extends Exception {

	private static final long serialVersionUID = 5658294469793692346L;
	/**
	 * pass errorMsg with custom text
	 * @param errorTxt
	 */
	public PreAggregateException(String errorTxt) {
		super(errorTxt);
	}
	/**
	 * default constructor
	 */
	public PreAggregateException() {
		super();
	}
}
