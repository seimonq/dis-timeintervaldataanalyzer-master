package net.kern.master.preaggregates.operations;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.kern.master.preaggregates.PreAggregateException;
import net.kern.master.preaggregates.PreAggregateModelHandler;
import net.kern.master.preaggregates.PreAggregateSelector;
import net.kern.master.preaggregates.PreAggregatedDescriptorMember;
import net.kern.master.preaggregates.PreAggregatedTimeMember;
import net.meisen.dissertation.model.data.DimensionModel;
import net.meisen.dissertation.model.data.IntervalModel;
import net.meisen.dissertation.model.data.IntervalModel.MappingResult;
import net.meisen.dissertation.model.data.MetaDataModel;
import net.meisen.dissertation.model.data.TidaModel;
import net.meisen.dissertation.model.datasets.IDataRecord;
import net.meisen.dissertation.model.descriptors.Descriptor;
import net.meisen.dissertation.model.descriptors.DescriptorModel;
import net.meisen.dissertation.model.dimensions.DescriptorDimension;
import net.meisen.dissertation.model.dimensions.DescriptorHierarchy;
import net.meisen.dissertation.model.dimensions.DescriptorMember;
import net.meisen.dissertation.model.dimensions.TimeHierarchy;
import net.meisen.dissertation.model.dimensions.TimeLevel;
import net.meisen.dissertation.model.dimensions.TimeLevelMember;
import net.meisen.dissertation.model.dimensions.TimeMemberRange;
import net.meisen.dissertation.model.dimensions.graph.DescriptorGraph;
import net.meisen.dissertation.model.dimensions.graph.DescriptorGraphNode;
import net.meisen.dissertation.model.dimensions.graph.IDimensionGraph;
import net.meisen.dissertation.model.dimensions.graph.TimeGraph;
import net.meisen.dissertation.model.indexes.datarecord.IntervalDataHandling;
import net.meisen.dissertation.model.indexes.datarecord.TidaIndex;
import net.meisen.dissertation.model.indexes.datarecord.slices.Bitmap;
import net.meisen.dissertation.model.indexes.datarecord.slices.Slice;
import net.meisen.dissertation.model.indexes.datarecord.slices.SliceWithDescriptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

	/**
	 * Arbeitspaket 2
	 * 
	 * This class provides methods to update preaggregates after new data was loaded
	 * preaggregates have to be updated if new records were added
	 * 
	 * in case records were deleted the current valid record index has to be and combined with preaggregated bitmap
	 *
	 *@author skern
	 */
	public class UpdatePreAggregates {
		
		private final static Logger LOG = LoggerFactory.getLogger(UpdatePreAggregates.class);
		private final TidaModel tidaModel;
		private final PreAggregateModelHandler paHandler;
		
		
		public UpdatePreAggregates(final TidaModel tidaModel, final PreAggregateModelHandler paHandler) throws PreAggregateException{
			
			if(tidaModel == null || paHandler == null) throw new PreAggregateException("one ore more parameters are null");
			
			this.tidaModel = tidaModel;
			this.paHandler = paHandler;
		}
		/**
		 * trigger method for updateProcess for descriptor graph members and time graph members
		 * 
		 * @param record
		 * 			newly loaded record
		 */
		public void updateAfterInsertion
			(IDataRecord record) {
			
			if(record != null) {
				final TidaIndex idx = tidaModel.getIndex();
				updateDescMember(record, idx);
				updateTimeMember(record, idx);
			}
		}
		/**
		 * when records were deleted this method updates all preaggregates by AND combining the two bitmaps
		 * 
		 * 
		 * @param validRecordBitmap#
		 * 		the currently valid index for all valid records
		 */
		public void updateAfterDeletion(Bitmap validRecordBitmap) {
				//update descriptorGraphNodes
			for(Map.Entry<PreAggregateSelector,PreAggregatedDescriptorMember> pa : 
					paHandler.getPreAggregatedDmList().entrySet()) {
				Bitmap oldBitmap = pa.getValue().getPreAggregate();
				pa.getValue().setPreAggregate(oldBitmap.and(validRecordBitmap));
			}
				//update timeGraphNodes
			for(Map.Entry<PreAggregateSelector,PreAggregatedTimeMember> pa : 
					paHandler.getPreAggregatedTlmList().entrySet()) {
				PreAggregatedTimeMember paTM = pa.getValue();
				for(Map.Entry<TimeMemberRange,Bitmap> paRange : paTM.getRanges().entrySet()) {
					Bitmap oldBitmap = paRange.getValue();
					paRange.setValue(oldBitmap.and(validRecordBitmap));
				}
			}
		}
		/** update method for all preaggregated non-temporal descriptor graph nodes 
		 * 
		 * @param record
		 * 			newly loaded record
		 * @param idx
		 * 			TidaIndex
		 */
		public void updateDescMember
		(final IDataRecord record, final TidaIndex idx) {

		final MetaDataModel metaModel = tidaModel.getMetaDataModel();
		final DimensionModel dimModel = tidaModel.getDimensionModel();
		final Collection<IDimensionGraph> dimGraphList = dimModel.getDimensions();

			//check for all descriptor values within record
		for(int i = 1; record.isValidPosition(i); i++) {			
			if(record.getValue(i) == null) {
				LOG.debug("value on pos "+i+" is null");
				if(record.getName(i) == null) {
					LOG.debug("name on pos "+i+" is null");
				}
				else LOG.debug("name on pos "+i+"is "+record.getName(i));
			continue;
			}
			
			final String value = record.getValue(i).toString(); 
			final String name = record.getName(i);

			if(LOG.isDebugEnabled()) 
				LOG.debug("record entry 'name':  '"+name+"' 'value':'"+value+"'");

				
			final DescriptorModel<?> descModel = metaModel.getDescriptorModel(name);
			if(descModel != null) {
					
				for(IDimensionGraph dimGraph : dimGraphList) {

						//update only non-temporal DescriptorMembers here
					if(dimGraph.getDimension() instanceof DescriptorDimension) {
						final DescriptorDimension descDim = (DescriptorDimension) dimGraph.getDimension();
						final DescriptorGraph descGraph = (DescriptorGraph) dimGraph;

							//find dimension and graph of descriptor model for current record position
						if(descModel.getId().equals(descDim.getDescriptorModelId())) {
							
								//check for all hierarchies
						    for(DescriptorHierarchy hry : descDim.getHierarchies()) {

					    		if(LOG.isDebugEnabled())
					    			LOG.debug("dimId: "+descDim.getId()+"hryId: "+hry.getId());
					    			
					    			//get all preaggregates within this dimension and hierarchy
					    		LinkedHashMap<PreAggregateSelector, PreAggregatedDescriptorMember> paMap = 
										paHandler.getPreAggregatedDmListByHierarchy(descDim.getId(), hry.getId());
					    			
					    			//check and update if necessary every found preaggregate
					    		for(Map.Entry<PreAggregateSelector, PreAggregatedDescriptorMember> pa : paMap.entrySet()) {
					    			DescriptorMember paMember = descGraph.getNode(hry.getId(), pa.getKey().getMemberId()).getMember();
					    			
					    			updatePANode(pa.getValue(), paMember, value, descGraph, hry, idx, descModel);
					    			
					    		}
						    }
						}
					}
				}
			}
		}
	}
		/**
		 * checks passed preaggregate and updates the bitmap inkrementally in case
		 * 
		 * either pa is a preaggrgate of a leaf descriptor node 
		 * 		then it has to be updated if the leaf contains the recordValue
		 * 
		 * or
		 * 
		 * pa is a preaggregate of a non-leaf descriptor node
		 * 		then all reachable leafs have to be checked
		 *  
		 * 
		 * @param pa
		 * 		the preaggregate which has to be checked
		 * @param member
		 * 		the current DescriptorMember which has to be checked whether the recordValue is covered or not
		 * @param recordValue
		 * 		the newly added recordValue to the system
		 * @param descGraph
		 * @param hry
		 * @param idx
		 * @param descModel
		 * 		the model the recordValue is part of
		 * 
		 */
		private void updatePANode
			(final PreAggregatedDescriptorMember pa, final DescriptorMember member, 
			 final String recordValue, final DescriptorGraph descGraph,
			 final DescriptorHierarchy hry, final TidaIndex idx, 
			 final DescriptorModel<?> descModel) {
				
				DescriptorGraphNode node = descGraph.getNode(hry.getId(), pa.getPaSelector().getMemberId());
				Descriptor<?,?,?> desc = descModel.getDescriptorByValue(recordValue);

					//if it is patterBased it is a leaf node and can be updated immediatly
				if(member.isPatternBased()) {
					if(member.containsValue(recordValue)) {
						//retrieve bitmap from descriptor index
					Slice<?> slice = idx.getMetaIndexDimensionSlice(descModel.getId(), desc.getId());
					Bitmap freshBitmap = slice.getBitmap();
						//update bitmap by combining it with updated descriptor index
					pa.setPreAggregate(pa.getPreAggregate().or(freshBitmap));

					}
					else {
						//this leaf is not affected
					}
				}
					//if it is not a leaf node get the children first
				else {
						//check all reachable leafs, if a leaf has changed
						//the dependent parent node has to be updated
					for(DescriptorMember leafMember : node.getReachableLeafs(hry.getId())) {
						updatePANode(pa,leafMember,recordValue,descGraph,hry,idx,descModel);
					}
				}
		
		}
		
		/**
		 * update time level member bottom up
		 * search for timelevelmember on every level which are affected by the changed range
		 * 
		 * @param record
		 * @param idx
		 */
		public void updateTimeMember
			(final IDataRecord record, final TidaIndex idx) {
		
			final DimensionModel dimModel = tidaModel.getDimensionModel();
			final Collection<IDimensionGraph> dimGraphList = dimModel.getDimensions();

			for(IDimensionGraph dimGraph : dimGraphList) {
					//selectTimeDimension
				if(dimGraph instanceof TimeGraph) {
					final TimeGraph timeGraph = (TimeGraph) dimGraph;

						//get mapped values from timeline definition
					final IntervalModel intervalModel = tidaModel.getIntervalModel();
					final IntervalDataHandling idh = tidaModel.getIntervalDataHandling();
					final MappingResult mappingResult = intervalModel.mapToTimeline(record.getValue("[START]"), record.getValue("[END]"), idh);
						//timepoint slices within this range have changed
					final TimeMemberRange range = new TimeMemberRange(mappingResult.getStart(),
									mappingResult.getEnd());
							
						//get TimeHierarchies
					final Collection<TimeHierarchy> hryList = timeGraph.getDimension().getHierarchies();
					
					for(TimeHierarchy hry : hryList) {

						//get TimeMemberPreAggregates for this hry
					final LinkedHashMap<PreAggregateSelector,PreAggregatedTimeMember> paList = 
							paHandler.getPreAggregatedTlmListByHierarchy
							(timeGraph.getDimension().getId(), hry.getId());

							//check each time level on preaggregates
						for(int i = hry.sizeOfLevels() - 1; i >= 0 ; i--) {

							final TimeLevel tl = hry.getLevel(i);

								//get affected TimeLevelMember
							final Collection<TimeLevelMember> tlmList = timeGraph.getMembers
									(hry.getId(), tl.getId(), range.getStart(), range.getEnd());
							if(tlmList != null) {
									//check all preaggregates of this hierarchy
								for(Map.Entry<PreAggregateSelector, PreAggregatedTimeMember> pa : paList.entrySet()) {
									for(TimeLevelMember tlm : tlmList) {
										
											//if this timelevelmember is preaggregated check it 
										if(pa.getKey().getLevelId().equals(tl.getId()) && 
											pa.getKey().getMemberId().equals(tlm.getId())) {
											
											if(LOG.isDebugEnabled()) LOG.debug("pa is outdated '"+pa.getKey().toString()+"'");
													//deprecated preaggregate has to be updated
												updateLevelMember(pa.getValue(), range);
										}
									}
								}
							}
						}
					}
				}
			}			
		}
		
		/** incremental update of all ranges which overlap with range of new indexed record
		 * 
		 * @param pa
		 * 		preaggregated TimeLevelMember
		 * @param range
		 * 		range of newly indexed record
		 */
		private void updateLevelMember(final PreAggregatedTimeMember pa, final TimeMemberRange range) {
			
			final TidaIndex idx = tidaModel.getIndex();
			
			
				//update all ranges of timelevelmember
			for(Map.Entry<TimeMemberRange, Bitmap> paRange : pa.getRanges().entrySet()) {

				//Bitmap bitmap = new RoaringBitmap();
				final TimeMemberRange tmr = paRange.getKey();
				LOG.debug("checking range: "+tmr.getStart()+" - "+tmr.getEnd());
				
					//check if ranges overlap with affected range of newly loaded record 
				if(tmr.getStart() <= range.getEnd() && tmr.getEnd() >= range.getStart()) {
					LOG.debug("interval overlaps");

						//find one slice the range of the preaggregate and the new record have in common
					long firstCommonTimePoint = Long.max(range.getStart(),tmr.getStart()); 

						//get only one Bitmap where the new record is included
					final SliceWithDescriptors<?>[] slices = idx.getIntervalIndexSlices(firstCommonTimePoint, firstCommonTimePoint);
					
						//update range incrementally by combining it with a bitmap where the new record is included
					paRange.setValue(paRange.getValue().or(slices[0].getBitmap()));
				}}	

		}
}
		
