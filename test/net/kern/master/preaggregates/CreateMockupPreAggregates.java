package net.kern.master.preaggregates;


import java.util.ArrayList;
import java.util.Collection;

import net.kern.master.preaggregates.querylog.QueryLogger;
import net.meisen.dissertation.config.TestConfig;
import net.meisen.dissertation.impl.parser.query.DimensionSelector;
import net.meisen.dissertation.model.data.DimensionModel;
import net.meisen.dissertation.model.data.TidaModel;
import net.meisen.dissertation.model.dimensions.TimeLevelMember;
import net.meisen.dissertation.model.dimensions.graph.IDimensionGraph;
import net.meisen.dissertation.model.dimensions.graph.TimeGraph;
import net.meisen.dissertation.model.handler.TidaModelHandler;
import net.meisen.general.sbconfigurator.runners.JUnitConfigurationRunner;
import net.meisen.general.sbconfigurator.runners.annotations.ContextClass;
import net.meisen.general.sbconfigurator.runners.annotations.ContextFile;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * helper method for all tests to create some mockup preaggregate sets
 * 
 * @author skern
 *
 */

@RunWith(JUnitConfigurationRunner.class)
@ContextClass(TestConfig.class)
@ContextFile("test-sbconfigurator-core.xml")

public class CreateMockupPreAggregates {

	
	private final static Logger LOG = LoggerFactory.getLogger(CreateMockupPreAggregates.class);
	private final PreAggregateModelHandler paModelHandler;
	private final QueryLogger queryLogger;
	/**
	 * constructor
	 * 
	 * @param model
	 */
	public CreateMockupPreAggregates(TidaModel model) {
		// load the model and initial data
		model.bulkLoadDataFromDataModel();

		//init preaggregate handler
		paModelHandler = new PreAggregateModelHandler();
		model.getDimensionModel().setPaModelHandler(paModelHandler);
		//init query logger
		queryLogger = new QueryLogger();
		model.getDimensionModel().setQueryLogger(queryLogger);
	}
	/**
	 * creates temporal and non temporal preaggregates within Caller Dataset
	 * @param model
	 */
	public void createMixedMockupCaller(TidaModel model) {
		LOG.debug("createMockup started");
		Collection<PreAggregateSelector> paList = new ArrayList<>();
			//1 descriptor member
		PreAggregateSelector paSel = new PreAggregateSelector("Caller","Physical",
				"Gender","Female");
		paList.add(paSel);
			//2 descriptor member
		paSel = new PreAggregateSelector("Caller","Physical",
				"Gender","Male");
		paList.add(paSel);
			//wrong member
		paSel = new PreAggregateSelector("Fake","Is",
				"Not","Real");
		paList.add(paSel);
		
			//3 timeMember
		
		Collection <TimeLevelMember> tlmList = null;
		DimensionSelector dimSel =  new DimensionSelector("TIME", "RASTER", "DAY");

		DimensionModel dimModel = model.getDimensionModel();
		
		for(IDimensionGraph dimGraph : dimModel.getDimensions()){
			if(dimGraph instanceof TimeGraph) {
				TimeGraph timeGraph = (TimeGraph)dimGraph;
					//for the whole timeline
				tlmList = timeGraph.getMembers("RASTER", "HOUR", 
						model.getIndex().getNormalizedTimeStart(), 
						model.getIndex().getNormalizedTimeEnd());
			}
		}
			//preaggregate all member
		for(TimeLevelMember tlm : tlmList) {
			paSel = new PreAggregateSelector(dimSel, tlm.getId());
			paList.add(paSel);
		}
		
		setPreaggregates(model, paList);

	}
	/**
	 * used to test performance gain within Caller Dataset
	 * 
	 * @param model
	 */
	public void createMockupCallerDM(TidaModel model) {
		LOG.debug("createMockup started");
		Collection<PreAggregateSelector> paList = new ArrayList<>();
			//1 descriptor member
		PreAggregateSelector paSel = new PreAggregateSelector("Caller","Physical",
				"Gender","Female");
		paList.add(paSel);
			//2 descriptor member
		paSel = new PreAggregateSelector("Caller","Physical",
				"Gender","Male");
		paList.add(paSel);
		
		setPreaggregates(model, paList);

	}
	/**
	 * tests preaggregates within complex hierarchies in the Caller Dataset
	 * @param model
	 */
	public void createNonStrictPreaggregatesCaller(TidaModel model) {
		
		LOG.debug("createNonStrictMockup started");
		Collection<PreAggregateSelector> paList = new ArrayList<>();

			//preaggregates for CallReason
		String[] memberList = {"complaint","order","information","changeOfAddress"};
		for(String member : memberList) {
		PreAggregateSelector paSel = new PreAggregateSelector("Caller","Advertisement",
				"CallReason",member);
		paList.add(paSel);
		}
	
		setPreaggregates(model, paList);

	}
	/**
	 * create preaggregates for time member in the TIDA Dataset
	 * 
	 * @param model
	 * @param level
	 */
	public void createMockupTidaTM(TidaModel model, String level) {
		LOG.debug("createMockup started");
		PreAggregateSelector paSel;
		Collection<PreAggregateSelector> paList = new ArrayList<>();
		Collection <TimeLevelMember> tlmList = null;
		DimensionSelector dimSel =  new DimensionSelector("TIME", "RASTER", level);
		DimensionModel dimModel = model.getDimensionModel();
		
		for(IDimensionGraph dimGraph : dimModel.getDimensions()){
			if(dimGraph instanceof TimeGraph) {
				TimeGraph timeGraph = (TimeGraph)dimGraph;
					//for the whole timeline
				tlmList = timeGraph.getMembers("RASTER", level, 
						model.getIndex().getNormalizedTimeStart(), 
						model.getIndex().getNormalizedTimeEnd());
			}
		}
			//preaggregate all member of level
		for(TimeLevelMember tlm : tlmList) {
			paSel = new PreAggregateSelector(dimSel, tlm.getId());
			paList.add(paSel);
		}
		
		setPreaggregates(model, paList);

	}
	/**
	 * helper method to trigger the creation process
	 * 
	 * @param model
	 * @param paList
	 * 		holds the identifier for the preaggregates
	 */
	public void setPreaggregates(TidaModel model, Collection<PreAggregateSelector> paList) {
		
		Collection<PreAggregateSelector> paSelectors = new ArrayList<>();
		
		if(paList != null) {
			for(PreAggregateSelector paSel : paList) {
				//set preaggregates
			paSelectors.add(paSel);
			}
			//create
			model.getDimensionModel().createPreAggregates(paSelectors);
		}
	}

	/**
	 * unload specified model
	 * @param model
	 * @param loader
	 */
	public void destroyModel(TidaModel model, TidaModelHandler loader) {
		
		// unload the model again
		loader.unloadAll();
	}		
}
	
