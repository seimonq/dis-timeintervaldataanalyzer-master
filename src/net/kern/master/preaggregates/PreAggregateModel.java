package net.kern.master.preaggregates;


/**
 * Abstract class for a PreAggregateModel
 * 
 * @author skern
 */

public abstract class PreAggregateModel {
	
		//identifier
	protected PreAggregateSelector paSelector;
	
	public PreAggregateSelector getPaSelector() {
		return paSelector;
	}
	public void setPaSelector(PreAggregateSelector paSelector) {
		this.paSelector = paSelector;
	}

}