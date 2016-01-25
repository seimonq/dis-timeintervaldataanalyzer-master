package net.kern.master.preaggregates.operations;

import java.util.HashMap;

import net.kern.master.preaggregates.PreAggregateException;
import net.kern.master.preaggregates.PreAggregateModelHandler;
import net.kern.master.preaggregates.PreAggregateSelector;
import net.kern.master.preaggregates.PreAggregatedDescriptorMember;
import net.kern.master.preaggregates.PreAggregatedTimeMember;
import net.meisen.dissertation.impl.indexes.datarecord.slices.RoaringBitmap;
import net.meisen.dissertation.impl.parser.query.DimensionSelector;
import net.meisen.dissertation.impl.parser.query.select.DimensionComperator;
import net.meisen.dissertation.model.data.DimensionModel;
import net.meisen.dissertation.model.data.DimensionModel.IMemberFilter;
import net.meisen.dissertation.model.dimensions.DescriptorMember;
import net.meisen.dissertation.model.dimensions.TimeHierarchy;
import net.meisen.dissertation.model.dimensions.TimeLevelMember;
import net.meisen.dissertation.model.dimensions.TimeMemberRange;
import net.meisen.dissertation.model.dimensions.graph.DescriptorGraph;
import net.meisen.dissertation.model.dimensions.graph.DescriptorGraphNode;
import net.meisen.dissertation.model.dimensions.graph.TimeGraph;
import net.meisen.dissertation.model.indexes.datarecord.slices.Bitmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arbeitspaket 3
 * 
 * this class selects and returns fully preaggregated bitmaps for 
 * {@code DescriptorGraphNode} or ranges of {@code TimeLevelMember}
 * if available or null
 * 
 * @author skern
 */
public class SelectPreAggregates {

	private final static Logger LOG = LoggerFactory
			.getLogger(SelectPreAggregates.class);
	
	private final PreAggregateModelHandler paHandler;
	private final DimensionModel dimModel;

		/**
		 * constructor
		 * @param paHandler
		 * @param dimModel
		 * @throws PreAggregateException
		 */
	public SelectPreAggregates(final PreAggregateModelHandler paHandler, final DimensionModel dimModel) throws PreAggregateException{
	
		if(dimModel == null || paHandler == null) throw new PreAggregateException("One or more of the passed parameters is null");

		this.paHandler = paHandler;
		this.dimModel = dimModel;
		
	}
	
	/**
	 * Method to select preaggregate for DescriporMember
	 * 
	 * retrieves Bitmap either directly or reuses preaggregates from lower level
	 * it can combine preaggregates from lower level and descriptor bitmaps from index
	 * if no preaggregate was found
	 * 
	 * @param dimComp
	 * 	contains {@code DimensionSelector} and exactly one memberId
	 * 
	 * @return always a Bitmap 
	 */
	public Bitmap useDescriptorMember(final DimensionComperator dimComp) throws PreAggregateException {
		
		if(dimComp == null) throw new PreAggregateException("Passed DimensionComperator is null");
		
		final String dimId = dimComp.getDimension().getDimensionId();
		final String hryId = dimComp.getDimension().getHierarchyId();

			//first scenario: preaggregate exists for exact comperator
		
		PreAggregateSelector paSel = new PreAggregateSelector(dimComp.getDimension().getDimensionId(),
				dimComp.getDimension().getHierarchyId(),dimComp.getDimension().getLevelId(),dimComp.getValue());
		
		PreAggregatedDescriptorMember paMem = (PreAggregatedDescriptorMember)paHandler.getPreAggregateBySelector(paSel);
			if(paMem != null) {
				if(LOG.isDebugEnabled()) LOG.debug("exact PA found with: "+paSel.toString());
				return paMem.getPreAggregate();
			}
			else {
				//continue search for preaggregates along graph
			}
		
			
			//second scenario: search preaggregates of successor nodes
			
			//wheneven no aggregate is found for a graph branch use the bitmap of the leaf 
			//(indexed descriptor member) finally 
			//bitmaps wil be "or" combined
			
		final HashMap<PreAggregateSelector,PreAggregatedDescriptorMember> paList = 
				paHandler.getPreAggregatedDmListByHierarchy(dimId,hryId);

			//no preaggregates found for this dimension and hierarchy return null
		if(paList.isEmpty()) {
			if(LOG.isDebugEnabled()) LOG.debug("no preaggregates found for dimId '"+dimId+"' and hryId '"+hryId+"'");
			return null;
		}

			//get bitmaps from lower levels of hierarchy
		final DescriptorGraph descGraph = (DescriptorGraph) 
				dimModel.getDimension(dimComp.getDimension().getDimensionId());
		final DescriptorGraphNode first = descGraph.getNode
				(dimComp.getDimension().getHierarchyId(), dimComp.getValue());
		Bitmap bitmap = new RoaringBitmap();
				//combine bitmaps from lower levels starting with the given DimensionComperator
			return useChildDescriptorNodes(bitmap, first, paList, dimId, hryId);
			
	}
	/**
	 * search preaggregates along the graph
	 * whenever no preaggregate is found descend further and use 
	 * Bitmaps from index as a last option
	 * 
	 * @param bitmap
	 * @param node
	 * @param paList
	 * @param dimId
	 * @param hryId
	 * 
	 * @return complete result
	 */
	private Bitmap useChildDescriptorNodes(
			Bitmap bitmap,final DescriptorGraphNode node,  
			final HashMap<PreAggregateSelector,PreAggregatedDescriptorMember> paList, 
			final String dimId, final String hryId) {
		
		for(final DescriptorGraphNode child : node.getChildren()) {
			boolean childUsed = false;
				//search for preaggregate for this child node
			for(HashMap.Entry<PreAggregateSelector,PreAggregatedDescriptorMember> pa : paList.entrySet()) {
					//use preaggregate if available 
					if(child.getMember().getId().equals(pa.getKey().getMemberId())) {
			
						bitmap = bitmap.or(pa.getValue().getPreAggregate());
						childUsed = true;
						if(LOG.isDebugEnabled()) 
							LOG.debug("catch bitmap from pa with id '"+child.getMember().getId()+"': "+bitmap.toString());
					}
			}

			//now three options exist:
			
				// 1: child is not preaggregated but has other children, which might be preaggregated
			if(!childUsed && !child.getChildren().isEmpty()) {
				LOG.debug("go deeper from node with id '"+child.getMember().getId()+"'");
				bitmap = useChildDescriptorNodes(bitmap, child, paList, dimId, hryId);
			}
				//2: child is not preaggregated and is leaf
			else if(!childUsed && child.getChildren().isEmpty()) {
					//get bitmap from descriptor index
				final DimensionSelector dimSel = new DimensionSelector(dimId, hryId, child.getLevel().getId());
				bitmap = bitmap.or(dimModel.getBitmap(dimSel, true, new IMemberFilter() {
					
					@Override
					public boolean accept(DescriptorMember member) {
						return child.getMember().getId().equals(member.getId());
					}
		
					@Override
					public boolean selectOnlyOne() {
						return true;
					}
				}));
				LOG.debug("catch bitmap from leaf with id '"+child.getMember().getId()+"': "+bitmap.toString());
			}
			else {
				//3: bitmap has been taken from preaggregated DescriptorMember 
			}
			
		}
		LOG.debug("final/intermediate bitmap: "+bitmap.toString());
		return bitmap;
	}
	
	
	/**
	 * check availability of TimeLevelMember preaggregates
	 * 
	 * @param dimSel
	 * @param member
	 * 
	 * @return most closest level of time hierarchy to the passed timelevelmember where preaggregates exist 
	 * 		   or -1 if no preaggregates exist 
	 */
	
	public int timeMemberAvailable(DimensionSelector dimSel, final TimeLevelMember member) {
		if(dimSel == null || member == null) return -1;
		
		final String dimId = dimSel.getDimensionId();
		final String hryId = dimSel.getHierarchyId();
		String lvlId;
		final TimeGraph timeGraph = (TimeGraph)dimModel.getDimension(dimSel.getDimensionId());
		final TimeHierarchy timeHry = timeGraph.getDimension().getHierarchy(dimSel.getHierarchyId());
		
		for(int i = 0; i < timeHry.sizeOfLevels(); i++) {
			if(timeHry.getLevel(i).getId().equals(dimSel.getLevelId())) {
				//u is now the level of the passed TimeLevelMember
				//search for preaggregates on this layer or lower
				for(int u = i; u < timeHry.sizeOfLevels(); u++) {
					lvlId = timeHry.getLevel(u).getId();
					dimSel = new DimensionSelector(dimId, hryId, lvlId); 
					if(!paHandler.getPreAggregatedTlmByLevel(dimSel).isEmpty()) {
						if(LOG.isDebugEnabled()) LOG.debug("timeLevelFound: "+u);
						//the number of the level
						return u;
					}
				}
			break;	
			}
		}
			//no preaggregates exist for this hierarchy 
		return -1;
	}
	
	/**
	 * function will return Bitmap for  TimeLevelRange
	 * 
	 * @param member
	 * 		TimeLevelMember
	 * @param range
	 * 		is a complete range of the passed TimeLevelMember
	 * @param dimSel
	 * 
	 * @param levelPos
	 * 		where a preaggregate was found by the method timeMemberAvailable
	 * 
	 * @return complete preaggregated bitmap
	 */
	public Bitmap useTimeMember(final TimeLevelMember member, final TimeMemberRange range, 
			final DimensionSelector dimSel, final int levelPos) {
		
		LOG.debug("useTimeMember started");

		Bitmap paResult = null; // = new PreAggregatedTimeMemberResult();

		PreAggregateSelector paSel = new PreAggregateSelector(dimSel, member.getId());
		
		PreAggregatedTimeMember pa = 
				(PreAggregatedTimeMember) paHandler.getPreAggregateBySelector(paSel);

			//get bitmap from preaggregated {@code TimeLevelMember}
		if(pa != null) {
			LOG.debug("aggregated bitmap used straight: "+pa.getRanges().get(range).toString());
			return pa.getRanges().get(range);
		}
			//if there is no exact match use bitmaps from the level where preaggregates were found
		else {
			final TimeGraph timeGraph = (TimeGraph)dimModel.getDimension(dimSel.getDimensionId());
			final TimeHierarchy timeHry = timeGraph.getDimension().getHierarchy(dimSel.getHierarchyId());
		
				//go to level
			String lvlId = timeHry.getLevel(levelPos).getId();
			final DimensionSelector selector = new DimensionSelector
					(dimSel.getDimensionId(), timeHry.getId(), lvlId);
			paSel = new PreAggregateSelector(selector, null);
				
				//get all member and ranges
			for(TimeLevelMember tlm : dimModel.getTimeMembers(selector, range.getStart(), range.getEnd())) {
				for(TimeMemberRange interval : tlm.getRanges()) {
						//if an found range is within the requested TimeMemberRange use it and or combine it
						//since only exact matching hierarchies are allowed the aggregate is fully combined
						//after these for loops
					if(interval.getStart() >= range.getStart() && interval.getEnd() <= range.getEnd()) {
						paSel.setMemberId(tlm.getId());
						pa = (PreAggregatedTimeMember) paHandler.getPreAggregateBySelector(paSel);
						if(paResult == null) paResult = pa.getRanges().get(interval);
						else paResult = paResult.or(pa.getRanges().get(interval));
					}
				}
			}
			
				//return bitmap - must not be null since passed levelPosition is the return of {@code timeMemberAvailable}
			return paResult;
		}
	}
	
	
}