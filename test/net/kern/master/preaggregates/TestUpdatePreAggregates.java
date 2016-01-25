package net.kern.master.preaggregates;

import java.util.HashMap;

import net.meisen.dissertation.config.TestConfig;
import net.meisen.dissertation.model.data.TidaModel;
import net.meisen.dissertation.model.dimensions.TimeMemberRange;
import net.meisen.dissertation.model.handler.TidaModelHandler;
import net.meisen.dissertation.model.indexes.datarecord.slices.Bitmap;
import net.meisen.general.sbconfigurator.api.IConfiguration;
import net.meisen.general.sbconfigurator.runners.JUnitConfigurationRunner;
import net.meisen.general.sbconfigurator.runners.annotations.ContextClass;
import net.meisen.general.sbconfigurator.runners.annotations.ContextFile;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@RunWith(JUnitConfigurationRunner.class)
@ContextClass(TestConfig.class)
@ContextFile("test-sbconfigurator-core.xml")
/**
 * tests functionality of incremental update
 * measures performance decrease due to update process
 * 
 * @author skern
 *
 */
public class TestUpdatePreAggregates {


		private final static Logger LOG = LoggerFactory.getLogger(TestUpdatePreAggregates.class);
		
		@Autowired(required = true)
		@Qualifier("coreConfiguration")
		private IConfiguration configuration;
		
		private TidaModel model;
		private CreateMockupPreAggregates init;	

		@Autowired
		private TidaModelHandler loader;
		
		@Before
		public void init() {
		//	model = loader
		//			.loadViaXslt("/net/kern/master/preaggregates/testCallerModel_1000.xml");
			model = loader
					.loadViaXslt("/net/kern/master/preaggregates/testCallerModel_10000.xml");
			init = new CreateMockupPreAggregates(model);
		}
		
		@Test
		public void testInsertionPAOn() {
			testModification("insert",true);
		}
		@Test
		public void testInsertionPAOff() {
			testModification("insert",false);
		}
		@Test
		public void testDeletionPAOn() {
			testModification("delete",true);
		}
		@Test
		public void testDeletionPAOff() {
			testModification("delete",false);
		}

		public void testModification(String type, boolean usePa) {
			
				//create mockup data 
			if(usePa) init.createMixedMockupCaller(model);

				//output all datasets
			Bitmap validBitmaps = model.getValidRecords();
			LOG.debug("alle Datensaetze: "+validBitmaps.toString());	
				//output preaggregates before modification
			PreAggregateModelHandler paHandler = model.getDimensionModel().getPaModelHandler();
			for(HashMap.Entry<PreAggregateSelector, PreAggregatedDescriptorMember> paDM : paHandler.getPreAggregatedDmList().entrySet()) {
				LOG.debug("selector '"+paDM.getKey().toString()+"' bitmap '"+paDM.getValue().getPreAggregate().toString()+"'");
				}
			for(HashMap.Entry<PreAggregateSelector, PreAggregatedTimeMember> paTM : paHandler.getPreAggregatedTlmList().entrySet()) {
				for(HashMap.Entry<TimeMemberRange,Bitmap> paTmr : paTM.getValue().getRanges().entrySet()) {
					LOG.debug("range '"+paTmr.getKey().toString()+"' bitmap '"+paTmr.getValue().toString()+"'");
				}
			}
	

			long start, end, result;
			result = start = end =  0;
			
				//delete some data
			final int[] ids = {1,12,31,44,52,56,71,89,90,91};
				//udpate index 
			start = System.currentTimeMillis();

			if(type.equals("delete")) 
				model.invalidateRecords(ids);
			else if(type.equals("insert")) 
				model.bulkLoadDataFromDataModel();

			end = System.currentTimeMillis();
			result += end - start;
			
			if(type.equals("delete")) LOG.debug("[type = delete]");
			else if(type.equals("insert")) LOG.debug("[type = insert]");
			
			if(usePa) LOG.debug("[preaggregates used]");
			else LOG.debug("[preaggregates not used]");
			LOG.debug("Das Update hat: "+result+" ms gedauert!");
				//output all valid data
			validBitmaps = model.getValidRecords();
			LOG.debug("alle Datensaetze: "+validBitmaps.toString());		

			paHandler = model.getDimensionModel().getPaModelHandler();
				//output preaggregates after modification
			for(HashMap.Entry<PreAggregateSelector, PreAggregatedDescriptorMember> paDM : paHandler.getPreAggregatedDmList().entrySet()) {
				LOG.debug("selector '"+paDM.getKey().toString()+"' bitmap '"+paDM.getValue().getPreAggregate().toString()+"'");
			}
			for(HashMap.Entry<PreAggregateSelector, PreAggregatedTimeMember> paTM : paHandler.getPreAggregatedTlmList().entrySet()) {
				for(HashMap.Entry<TimeMemberRange,Bitmap> paTmr : paTM.getValue().getRanges().entrySet()) {
					LOG.debug("range '"+paTmr.getKey().toString()+"' bitmap '"+paTmr.getValue().toString()+"'");
				}
			}

			init.destroyModel(model, loader);
		}
		
	}

