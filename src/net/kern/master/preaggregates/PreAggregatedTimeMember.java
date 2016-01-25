package net.kern.master.preaggregates;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.meisen.dissertation.impl.indexes.datarecord.slices.RoaringBitmap;
import net.meisen.dissertation.model.dimensions.TimeMemberRange;
import net.meisen.dissertation.model.indexes.datarecord.slices.Bitmap;

/**
 * Model used for preaggregates for TimeLevelMember which are associated to the TimeGraphNodes
 * 
 * a model contains a list of all ranges which hold the bitmaps
 * 
 * @author skern
 */
public class PreAggregatedTimeMember extends PreAggregateModel {

	private LinkedHashMap<TimeMemberRange,Bitmap> paRanges;
	
	/**
	 * default constructor
	 */
	public PreAggregatedTimeMember() {
		paRanges = new LinkedHashMap<>();
		paSelector = null;
	}
	/** stores for all passed ranges for the specified time graph node the passed bitmaps 
	 * 
	 * @param paSel
	 * @param preAggregate
	 */
	public void createPreAggregate
		(final PreAggregateSelector paSel, final LinkedHashMap<TimeMemberRange,Bitmap> preAggregate) {

		this.paRanges 	= new LinkedHashMap<TimeMemberRange,Bitmap> (preAggregate);
		this.paSelector = paSel;
		
	}
	/**
	 * updates existing bitmaps for all ranges passed
	 * 
	 * @param preAggregate
	 */
	public void updatePreAggregate(final LinkedHashMap<TimeMemberRange,Bitmap> preAggregate) {
		this.paRanges = new LinkedHashMap<TimeMemberRange,Bitmap> (preAggregate);
	}
	/**
	 * sets new bitmap for specified range
	 * 
	 * @param bitmap
	 * @param range
	 */
	public void addOneTimeRangePreAggregate(final Bitmap bitmap, final long[] range) {
		TimeMemberRange paRange = new TimeMemberRange(range[0], range[1]);
		paRanges.put(paRange, bitmap);	
	}
	/**
	 * 
	 * @param range
	 * 		requested range
	 * @return select specific bitmap
	 */
	public Bitmap getOneTimeRangePreAggregate(final TimeMemberRange range) {
		Bitmap result = paRanges.get(range);
		return result;
	}
	/**
	 * 
	 * @return all Bitmaps for available ranges
	 */
	public LinkedHashMap<TimeMemberRange,Bitmap> getRanges() {
		return paRanges;
	}
	/**
	 * reset all ranges
	 */
	public void flushAggregates() {
		final Iterator<Entry<TimeMemberRange, Bitmap>> it = paRanges.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<TimeMemberRange,Bitmap> aggregate = (Map.Entry<TimeMemberRange,Bitmap>)it.next();
			aggregate.setValue(new RoaringBitmap()); 
		}
	}
	
}