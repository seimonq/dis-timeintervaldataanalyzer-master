package net.kern.master.preaggregates.querylog;

import java.util.ArrayList;
import java.util.List;

import net.meisen.dissertation.impl.parser.query.DimensionSelector;
import net.meisen.dissertation.impl.parser.query.select.DimensionComperator;
import net.meisen.dissertation.impl.parser.query.select.IComperator;
import net.meisen.dissertation.impl.parser.query.select.logical.DescriptorLeaf;
import net.meisen.dissertation.impl.parser.query.select.logical.DescriptorLogicTree;
import net.meisen.dissertation.impl.parser.query.select.logical.ILogicalTreeElement;
/**
 * this class is used as a datatype for logging SELECT TIMESERIES Querys
 *
 * @author skern
 *
 */
public class QueryLog {
	/**
	 * contains all used Dimension Expressions within this query 
	 */
	private List<DimensionComperator> dimCompList;
	/**
	 * contains Measure Expressions
	 */
	private DimensionSelector timeLevel;
	
	public QueryLog() {
		dimCompList = new ArrayList<>();
	}
	/**
	 * create complete log
	 * 
	 * @param descFilter
	 * @param dimSel
	 */
	public QueryLog(DescriptorLogicTree descFilter, DimensionSelector dimSel) {

		dimCompList = new ArrayList<>();
		for(ILogicalTreeElement elem : descFilter.getEvaluationOrder()) {
			if(elem instanceof DescriptorLeaf) {
				IComperator comp = ((DescriptorLeaf) elem).get();
				if(comp instanceof DimensionComperator) {
					dimCompList.add((DimensionComperator)comp);
				}
			}
		}

		timeLevel = dimSel;
	}
	public boolean isSet() {
		if(dimCompList.isEmpty() && timeLevel == null) return false;
		else return true;
	}
	/**
	 * 
	 * @return List of Dimension Expressions
	 */
	public List<DimensionComperator> getDescriptorFilter() {
		return dimCompList;
	}
	/**
	 * 
	 * @param compList
	 * 		dimensional Filter conditions of Query
	 */
	public void setDescriptorFilter(List<DimensionComperator> compList) {
		this.dimCompList = compList;
	}
	/**
	 * 
	 * @return Measure Expression
	 */
	public DimensionSelector getTimeLevel() {
		return timeLevel;
	}
	/**
	 * set Measure Expression
	 * @param timeLevel
	 */
	public void setTimeLevel(DimensionSelector timeLevel) {
		this.timeLevel = timeLevel;
	}
	@Override
	public String toString() {
		String dimComps = " ";
		for(DimensionComperator comp : dimCompList) {
			dimComps += comp.toString()+" ";
		}
		return "timeLevel: "+timeLevel.toString()+" and descriptorFilter: "+dimComps;
	}
	
	
}
