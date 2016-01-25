package net.kern.master.preaggregates.querylog;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import net.meisen.dissertation.impl.parser.query.DimensionSelector;
import net.meisen.dissertation.impl.parser.query.select.DimensionComperator;
import net.meisen.dissertation.impl.parser.query.select.SelectQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arbeitspaket 4
 * 
 * This class can be used to log Filter conditions for Descriptor and Time Dimensions
 * and allows to retrieve an ordered list of the most often used conditions.
 * 
 * The statistical output is useful to determine which preaggregates are most relevant for the system. 
 *
 * @author skern
 *
 */
public class QueryLogger {

	private final static Logger LOG = LoggerFactory
			.getLogger(QueryLogger.class);
	
	private Collection<QueryLog> queryHistory;
	
	/**
	 * default constructor
	 */
	public QueryLogger() {
		queryHistory = new ArrayList<QueryLog>();
	}
	
	/**
	 * 
	 * @param selectQuery
	 * 		last select query which shall be logged
	 */
	public void logQuery(SelectQuery selectQuery) {
			//create new QueryLog
			//consists of a list of descriptor graph nodes and the time level
		QueryLog queryLog = new QueryLog(selectQuery.getFilter(), selectQuery.getMeasureDimension());
		if(queryLog.isSet()) {
			queryHistory.add(queryLog);
		}
		
		if(LOG.isDebugEnabled()) LOG.debug("queryLog: "+queryLog.toString());

	}
	/**
	 * 
	 * @return ordered Map
	 * 		contains all Descriptorgraph nodes which were used as a filter condition on a select query
	 * 		ordered by occurence
	 */
	public LinkedHashMap<DimensionComperator,Integer> getOrderedDescriptorFilters() {
		HashMap<DimensionComperator, Integer> occurrences = new HashMap<>();
		for(QueryLog entry : queryHistory) {
			if(entry.getDescriptorFilter() != null) {
				for(DimensionComperator comp : entry.getDescriptorFilter()) {
					if(occurrences.get(comp) == null) {
						occurrences.put(comp, 1);
					}
					else {
						Integer oldValue = occurrences.get(comp);
						occurrences.put(comp,++oldValue);
					}
				}
			}
		}
			//sort map
		LinkedHashMap<DimensionComperator, Integer> result = new LinkedHashMap<>();
		for(HashMap.Entry<?,Integer> entry : sortHashMap(occurrences).entrySet()) {
			result.put((DimensionComperator)entry.getKey(),entry.getValue());
		}
		return result;
	}
	/**
	 * 
	 * @return ordered Map
	 * 		contains all requested time dimension levels ordered by occurence
	 */
	public LinkedHashMap<DimensionSelector,Integer> getOrderedMeasureLevels() {
		HashMap<DimensionSelector, Integer> occurrences = new HashMap<>();
		for(QueryLog entry : queryHistory) {
			if(occurrences.get(entry.getTimeLevel()) == null) {
						occurrences.put(entry.getTimeLevel(), 1);
					}
					else {
						Integer oldValue = occurrences.get(entry.getTimeLevel());
						occurrences.put(entry.getTimeLevel(), ++oldValue);
					}
			}
			//sort map
		LinkedHashMap<DimensionSelector, Integer> result = new LinkedHashMap<>();
		for(HashMap.Entry<?,Integer> entry : sortHashMap(occurrences).entrySet()) {
			result.put((DimensionSelector)entry.getKey(),entry.getValue());
		}
		return result;
	}
	
	/**
	 * selection sort like sorting algorithm for the statistical output
	 * 
	 * @param unordered
	 * @return
	 */
	private LinkedHashMap<? extends Object,Integer> sortHashMap(HashMap<? extends Object,Integer> unordered) {

		LinkedHashMap<Object, Integer> result = new LinkedHashMap<>();
		while (unordered != null && !unordered.isEmpty()) {
			Integer max = 0;
			Object dimComp = null;
			for(HashMap.Entry<?, Integer> entry : unordered.entrySet()) {
				if(entry.getValue() > max) {
					max = entry.getValue();
					dimComp = entry.getKey();
				}
			}
			result.put(dimComp, max);
			unordered.remove(dimComp);
		}
		return result;
	}
	
}
