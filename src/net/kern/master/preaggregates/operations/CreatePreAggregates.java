package net.kern.master.preaggregates.operations;


import java.util.Set;

import net.kern.master.preaggregates.PreAggregateException;
import net.kern.master.preaggregates.PreAggregateModelHandler;
import net.kern.master.preaggregates.PreAggregateSelector;
import net.kern.master.preaggregates.PreAggregatedDescriptorMember;
import net.kern.master.preaggregates.PreAggregatedTimeMember;
import net.meisen.dissertation.impl.indexes.datarecord.slices.RoaringBitmap;
import net.meisen.dissertation.impl.parser.query.DimensionSelector;
import net.meisen.dissertation.model.data.DimensionModel;
import net.meisen.dissertation.model.data.DimensionModel.IMemberFilter;
import net.meisen.dissertation.model.dimensions.DescriptorMember;
import net.meisen.dissertation.model.dimensions.TimeLevelMember;
import net.meisen.dissertation.model.dimensions.TimeMemberRange;
import net.meisen.dissertation.model.indexes.datarecord.TidaIndex;
import net.meisen.dissertation.model.indexes.datarecord.slices.Bitmap;
import net.meisen.dissertation.model.indexes.datarecord.slices.SliceWithDescriptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arbeitspaket 1
 * 
 * This class preaggregates for one descriptor or time member a bitmap 
 * which contains all covered record ids by the passed selector
 * and stores it in a Hashmap maintained by the {@code PreAggregateModelHandler}  
 * 
 * @author skern
 */

public class CreatePreAggregates {

	private final static Logger LOG = LoggerFactory.getLogger(CreatePreAggregates.class);

	private final DimensionModel dimModel;
	private final PreAggregateModelHandler paHandler;
	private final TidaIndex tdx;
	
	/**
	 * @param dimModel
	 * 		class from where creation of preaggregates is triggered
	 * @param paHandler
	 * */
	public CreatePreAggregates(final DimensionModel dimModel, 
			final PreAggregateModelHandler paHandler, final TidaIndex tdx) 
			throws PreAggregateException {

		if(dimModel == null || paHandler == null || tdx == null) throw new PreAggregateException("One or more of the passed parameters is null");

		this.dimModel = dimModel;
		this.paHandler = paHandler;
		this.tdx = tdx;
	}
	/** 
	 * method to create one preaggregate for one descriptor member
	 * 
	 * @param paSel
	 * 		consists of {@code DimensionSelector} and a member id
	 */	
	public void createDescriptorMember (final PreAggregateSelector paSel) throws PreAggregateException {
		
		if(paSel == null) throw new PreAggregateException("Passed PreAggregateSelector is null");

		try {
					//retrieve combined Bitmap from dimensionModel
				final DimensionSelector dimSel = paSel.getDimensionSelector();
				if(dimSel == null) throw new PreAggregateException("DimensionSelector is null");
			
				final Bitmap bitmap =
					dimModel.getBitmap(dimSel, true, new IMemberFilter() {
					@Override
					public boolean accept(DescriptorMember member) {
						return paSel.getMemberId().equals(member.getId());
					}
					@Override
					public boolean selectOnlyOne() {
						return true;
				}});
	
				//do not store empty bitmaps
			if(bitmap != null && bitmap.isBitSet()) {
	
					//prepare new preaggregate 
				final PreAggregatedDescriptorMember pa = 
						new PreAggregatedDescriptorMember(paSel,bitmap);
					//save new preaggregate
				if(LOG.isDebugEnabled()) LOG.debug("preaggregate new descriptorMember: "+paSel.toString());

				paHandler.setDescriptorMemberPA(paSel, pa);
			}
		
		}catch(Exception e) {
			if(LOG.isErrorEnabled()) LOG.error("ErrorMessage: "+e.getMessage());
			throw new PreAggregateException();
		}
	}
	
	/** 
	 * create all ranges for the passed time level member
	 * 
	 * @param paSel
	 * 		consists of {@code DimensionSelector} and a member id from a valid TimeDimension
	 */		
	public void createTimeMember(final PreAggregateSelector paSel) throws PreAggregateException {

		if(paSel == null) throw new PreAggregateException("Passed PreAggregateSelector is null");

		try {
	
			Bitmap paBitmap = null;
			
				//pre-aggregates for time members are set for all related ranges 
				//interval dependend preaggregates are possible extension for future research
			long start = tdx.getNormalizedTimeStart();
			long end   = tdx.getNormalizedTimeEnd();
			
			final DimensionSelector dimSel = paSel.getDimensionSelector();
			final Set<TimeLevelMember> tlmList = dimModel.getTimeMembers(dimSel, start, end);
			
			for(final TimeLevelMember tlm : tlmList) {
					//find the passed member which will be preaggregated
				if(tlm.getId().equals(paSel.getMemberId())) {
					
					PreAggregatedTimeMember paTimeMember = new PreAggregatedTimeMember();
					paTimeMember.setPaSelector(paSel);
						//get all TimeRanges for the TimeLevelMember
					for (final TimeMemberRange tmr : tlm.getRanges()) {
	
						start = tmr.getStart();
						end   = tmr.getEnd();

							// get the slices of the range
						final SliceWithDescriptors<?>[] slices = tdx
								.getIntervalIndexSlices(start, end);
	
							// combine the slices 
						paBitmap = null;
						for (final SliceWithDescriptors<?> slice : slices) {
							if (slice == null) {
								continue;
							}
							paBitmap = Bitmap.combineBitmaps(paBitmap, slice);
						}
						final long[] range = {start,end};
	
							//add the ranges one by one to the preaggregates
							//in case the bitmap is null, create an empty bitmap
						if(paBitmap == null) paBitmap = new RoaringBitmap();
						paTimeMember.addOneTimeRangePreAggregate(paBitmap, range);
					}
						//add the final set to the handler with identifier
					if(LOG.isDebugEnabled()) LOG.debug("preaggregate new timeMember: "+paSel.toString());
					paHandler.setTimeMemberPA(paSel,paTimeMember);
				}
			}
		}catch(Exception e) {
			if(LOG.isErrorEnabled()) LOG.error("ErrorMessage: "+e.getMessage());
			throw new PreAggregateException();
		}			

	}
}
