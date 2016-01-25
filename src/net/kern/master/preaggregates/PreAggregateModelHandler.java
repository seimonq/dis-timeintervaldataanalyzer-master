package net.kern.master.preaggregates;

import java.util.LinkedHashMap;
import java.util.Map;

import net.meisen.dissertation.impl.parser.query.DimensionSelector;
/**
 * 
 * This handler maintains all preaggregates as a LinkedHashMap 
 * 
 * it contains setting, retrieving and deletion methods for PreAggregatedTimeMember and PreAggregatedDescriptorMember
 * 
 * @author skern
 */
public class PreAggregateModelHandler {
	
	private LinkedHashMap<PreAggregateSelector, PreAggregateModel> paList;
	/**
	 * default constructor
	 */
	public PreAggregateModelHandler() {
		paList = new LinkedHashMap<>();
	}
	/** get PreAggregateModel by Selector
	 * 
	 * @param paSel
	 * 		Identifier of the PreAggregateModel
	 * 
	 * @return one PreAggregateModel
	 * 
	 */
	public PreAggregateModel getPreAggregateBySelector(PreAggregateSelector paSel) {
		if(paList != null) {
			if(paList.containsKey(paSel)) {
				return paList.get(paSel);
			}
		}	
		return null;	
	}
	/**
	 * @return all available PreAggregateModels
	 */
	public LinkedHashMap<PreAggregateSelector, PreAggregateModel> getPreAggregatesList() {
		return paList;
	}
	/**
	 * @return only preaggregated time graph nodes
	 */
	public LinkedHashMap<PreAggregateSelector, PreAggregatedTimeMember> 
		getPreAggregatedTlmList() {
		LinkedHashMap<PreAggregateSelector, PreAggregatedTimeMember> result = new LinkedHashMap<>();
		for(Map.Entry<PreAggregateSelector, PreAggregateModel> pa : paList.entrySet()) {
			if(pa.getValue() instanceof PreAggregatedTimeMember) {
				result.put(pa.getKey(), (PreAggregatedTimeMember)pa.getValue());
			}
		}
		return result;
	}
	/**
	 * @return only preaggregated descriptor graph nodes
	 */
	public LinkedHashMap<PreAggregateSelector, PreAggregatedDescriptorMember> 
	getPreAggregatedDmList() {

	LinkedHashMap<PreAggregateSelector, PreAggregatedDescriptorMember> result = new LinkedHashMap<>();
	for(Map.Entry<PreAggregateSelector, PreAggregateModel> pa : paList.entrySet()) {
		if(pa.getValue() instanceof PreAggregatedDescriptorMember) {
			result.put(pa.getKey(), (PreAggregatedDescriptorMember)pa.getValue());
		}
	}
	return result;
}
	/**
	 * @param dimId
	 * @param hryId
	 * 
	 * @return time graph nodes of passed dimension an hierarchy
	 */
	public LinkedHashMap<PreAggregateSelector, PreAggregatedTimeMember> 
		getPreAggregatedTlmListByHierarchy(String dimId, String hryId) {
	
	LinkedHashMap<PreAggregateSelector, PreAggregatedTimeMember> result = new LinkedHashMap<>();
	for(Map.Entry<PreAggregateSelector, PreAggregateModel> pa : paList.entrySet()) {
		if(pa.getValue() instanceof PreAggregatedTimeMember &&
				pa.getKey().getDimensionId().equals(dimId) &&
				pa.getKey().getHierarchyId().equals(hryId)) {
			result.put(pa.getKey(), (PreAggregatedTimeMember)pa.getValue());
		}
	}
	return result;
	}
	/**
	 * @param dimId
	 * @param hryId
	 * 
	 * @return descriptor graph nodes of passed dimension an hierarchy
	 */
	public LinkedHashMap<PreAggregateSelector,PreAggregatedDescriptorMember> getPreAggregatedDmListByHierarchy
	(String dimId, String hryId) {

	LinkedHashMap<PreAggregateSelector, PreAggregatedDescriptorMember> result = new LinkedHashMap<>();
	for(Map.Entry<PreAggregateSelector, PreAggregateModel> pa : paList.entrySet()) {
		if(pa.getValue() instanceof PreAggregatedDescriptorMember
				&& pa.getKey().getDimensionId().equals(dimId)
				&& pa.getKey().getHierarchyId().equals(hryId)) {
			result.put(pa.getKey(), (PreAggregatedDescriptorMember)pa.getValue());
		}
	}
	return result;
}	
	/**
	 * 
	 * @param dimSel
	 * 		dimensionSelector contains Dimension, Hierarchy, Level
	 * @return only preaggregated time graph nodes from specified time graph level
	 */
	public LinkedHashMap<PreAggregateSelector,PreAggregatedTimeMember> getPreAggregatedTlmByLevel
	(DimensionSelector dimSel) {
		LinkedHashMap<PreAggregateSelector,PreAggregatedTimeMember> result = new LinkedHashMap<>();
		for(Map.Entry<PreAggregateSelector, PreAggregateModel> pa : paList.entrySet()) {
			if(pa.getKey().getDimensionSelector().equals(dimSel)) {
				result.put(pa.getKey(), (PreAggregatedTimeMember)pa.getValue());
			}
		}
		return result;
	}
	/**
	 * set new preaggregate for descriptor graph node
	 * 
	 * @param name
	 * @param paModel
	 */
	public void setDescriptorMemberPA(PreAggregateSelector name, PreAggregatedDescriptorMember paModel) {
		paList.put(name, paModel);
	}
	/**
	  * set new preaggregate for time graph node
	 * @param name
	 * @param paModel
	 */
	public void setTimeMemberPA(PreAggregateSelector name, PreAggregatedTimeMember paModel) {
		paList.put(name, paModel);
	}
	/**
	 * remove selected preaggregate if exists
	 * @param paSelector
	 */
	public void removePreAggregate(PreAggregateSelector paSelector) {
		if(paList.containsKey(paSelector)) 
			paList.remove(paSelector);
	}
}
