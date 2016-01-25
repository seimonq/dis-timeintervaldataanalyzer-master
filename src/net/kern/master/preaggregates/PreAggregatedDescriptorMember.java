package net.kern.master.preaggregates;

import net.meisen.dissertation.model.indexes.datarecord.slices.Bitmap;

/**
 * Model used for preaggregated descriptor graph nodes
 * 
 * @author skern
 */
public class PreAggregatedDescriptorMember extends PreAggregateModel {
	
	private Bitmap preAggregate;
	/**
	 * default constructor
	 */
	public PreAggregatedDescriptorMember() {
		preAggregate = null;
		paSelector   = null;
	}
	/**
	 * constructs complete preaggregate
	 * @param paSel
	 * @param preAggregate
	 */
	public PreAggregatedDescriptorMember
		(final PreAggregateSelector paSel, final Bitmap preAggregate) {

		paSelector   	  = paSel;
		this.preAggregate = preAggregate.copy();
	}
	/**
	 * creates new selector and bitmap
	 * @param paSel
	 * @param preAggregate
	 */
	public void createPreAggregate(final PreAggregateSelector paSel, final Bitmap preAggregate) {

		this.paSelector    = paSel;
		this.preAggregate = preAggregate.copy();
		
	}
	/**
	 * 
	 * @return the bitmap
	 */
	public Bitmap getPreAggregate() {
		return preAggregate;
	}
	/**
	 * resets the stored bitmap
	 * 
	 * @param bitmap
	 */
	public void setPreAggregate(final Bitmap bitmap) {
		preAggregate = bitmap.copy();
	}

}
