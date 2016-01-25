package net.kern.master.preaggregates;

import java.util.HashMap;

import net.kern.master.preaggregates.querylog.QueryLogger;
import net.meisen.dissertation.config.TestConfig;
import net.meisen.dissertation.config.xslt.DefaultValues;
import net.meisen.dissertation.impl.parser.query.DimensionSelector;
import net.meisen.dissertation.impl.parser.query.select.DimensionComperator;
import net.meisen.dissertation.model.data.TidaModel;
import net.meisen.dissertation.model.handler.TidaModelHandler;
import net.meisen.dissertation.model.parser.query.IQuery;
import net.meisen.dissertation.model.parser.query.IQueryFactory;
import net.meisen.dissertation.model.parser.query.IQueryResult;
import net.meisen.dissertation.server.sessions.SessionManager;
import net.meisen.general.sbconfigurator.api.IConfiguration;
import net.meisen.general.sbconfigurator.runners.JUnitConfigurationRunner;
import net.meisen.general.sbconfigurator.runners.annotations.ContextClass;
import net.meisen.general.sbconfigurator.runners.annotations.ContextFile;

import org.junit.After;
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
 * sends multiple querys to the system and counts the amount of used measure expressions and dimension expressions
 * 
 * @author seimonq
 *
 */
public class TestQueryLogger {

	private final static Logger LOG = LoggerFactory.getLogger(TestQueryLogger.class);
	
	private final int test_times = 10;
	
	@Autowired(required = true)
	@Qualifier("coreConfiguration")
	private IConfiguration configuration;
	
	private TidaModel model;
	private CreateMockupPreAggregates init;	

	@Autowired
	@Qualifier(DefaultValues.MODELHANDLER_ID)
	private TidaModelHandler loader;
	
	@Autowired
	@Qualifier(DefaultValues.QUERYFACTORY_ID)
	private IQueryFactory queryFactory;
	
	/**
	 * The used {@code SessionManager}.
	 */
	@Autowired
	@Qualifier(DefaultValues.SESSIONMANAGER_ID)
	protected SessionManager sessionManager;
	
	@Before
	public void init() {
		model = loader
				.loadViaXslt("/net/kern/master/preaggregates/testCallerModel_100.xml");
		init = new CreateMockupPreAggregates(model);

	}
	@After
	public void exit() {
	 init.destroyModel(model, loader);
	}
	
	@Test
	public void testQueryLogger() {
	
			//create mockup data
		init.createMixedMockupCaller(model); 
	
		String query = "SELECT TIMESERIES OF count(CALLER) ON TIME.RASTER.MONTH "
				+ "FROM datasetCaller IN [01.09.2013,25.10.2013] WHERE "
				+ "Caller.Physical.Gender = 'Male'";
		
		IQuery parsedQuery = queryFactory.parseQuery(query);
		IQueryResult resQuery;
		
			//fire x times the select query
		for(int i = 0; i < test_times ; i++) {
			resQuery = queryFactory.evaluateQuery(parsedQuery,
					sessionManager);
		}
		
		query = "SELECT TIMESERIES OF count(CALLER) ON TIME.RASTER.DAY "
				+ "FROM datasetCaller IN [01.09.2013,25.10.2013] WHERE "
				+ "Caller.Physical.Gender = 'Female'";
		parsedQuery = queryFactory.parseQuery(query);
		//fire x times the select query
		for(int i = 0; i < test_times + 10; i++) {
			resQuery = queryFactory.evaluateQuery(parsedQuery,
					sessionManager);
		}				

		QueryLogger log = model.getDimensionModel().getQueryLogger();
		
		for(HashMap.Entry<DimensionComperator,Integer> entry : log.getOrderedDescriptorFilters().entrySet()) {
			LOG.debug("filter :"+entry.getKey()+" ("+entry.getValue()+")");
		}
		for(HashMap.Entry<DimensionSelector,Integer> entry : log.getOrderedMeasureLevels().entrySet()) {
			LOG.debug("filter :"+entry.getKey()+" ("+entry.getValue()+")");
		}		

	}
}
