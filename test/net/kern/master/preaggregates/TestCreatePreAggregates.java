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
 * tests to create a mixed set of preaggregates
 * 
 * @author skern
 *
 */


public class TestCreatePreAggregates {

	private final static Logger LOG = LoggerFactory.getLogger(TestCreatePreAggregates.class);
	
	@Autowired(required = true)
	@Qualifier("coreConfiguration")
	private IConfiguration configuration;
	
	private TidaModel model;
	private CreateMockupPreAggregates init;	

	@Autowired
	private TidaModelHandler loader;
	
	@Before
	public void init() {
			
		model = loader
				.loadViaXslt("/net/kern/master/preaggregates/testCallerModel_100.xml");
		init = new CreateMockupPreAggregates(model);
	}
	
	@Test
	public void test() {
		
			//create some mixed mockup data

		init.createMixedMockupCaller(model); 
		
			//control debug message
		
		PreAggregateModelHandler paHandler = model.getDimensionModel().getPaModelHandler();
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
