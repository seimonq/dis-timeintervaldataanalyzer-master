package net.kern.master.preaggregates;

import net.meisen.dissertation.config.TestConfig;
import net.meisen.dissertation.config.xslt.DefaultValues;
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
 * Test Series 2 - Performance Test for Preaggregates for TimeLevelMember
 * 
 * @author seimonq
 *
 */
public class TestSelectPATimeMember {

	private final static Logger LOG = LoggerFactory.getLogger(TestSelectPATimeMember.class);
	
	private final int test_times = 50;
	
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

	@Autowired
	@Qualifier(DefaultValues.SESSIONMANAGER_ID)
	protected SessionManager sessionManager;
	
	@Before
	public void init() {

	//	model = loader
	//			.loadViaXslt("/net/kern/master/preaggregates/testTidaModel_100.xml");
	
		model = loader
				.loadViaXslt("/net/kern/master/preaggregates/testTidaModel_1000.xml");
	/*
		model = loader
				.loadViaXslt("/net/kern/master/preaggregates/testTidaModel_10000.xml");
		model = loader
				.loadViaXslt("/net/kern/master/preaggregates/testTidaModel_100000.xml");
	*/	
		init = new CreateMockupPreAggregates(model);

	}
	@After
	public void exit() {
	 init.destroyModel(model, loader);
	}

	
	@Test
	public void testPAOn() {
		String level = "DAY";
		String query = "SELECT TIMESERIES OF count(TASKTYPE) ON TIME.RASTER.DAY "
				+ "FROM datasetTida IN  [01.01.2008,10.02.2008] ";
		testTidaTimeMember(level,query,true);
	}
	@Test
	public void testPAOff() {
			//use preaggregates
		String level = "";
		String query = "SELECT TIMESERIES OF count(TASKTYPE) ON TIME.RASTER.DAY "
				+ "FROM datasetTida IN [01.01.2008,10.02.2008] ";
		testTidaTimeMember(level,query,false);
	}	
	@Test
	public void testPACombine() {
		String level = "HOUR";
		String query = "SELECT TIMESERIES OF count(TASKTYPE) ON TIME.RASTER.DAY "
				+ "FROM datasetTida IN  [01.01.2008,10.02.2008] ";
		testTidaTimeMember(level,query,true);
	}
	@Test
	public void testPACombineOff() {
			//use preaggregates
		String level = "";
		String query = "SELECT TIMESERIES OF count(TASKTYPE) ON TIME.RASTER.DAY "
				+ "FROM datasetTida IN [01.01.2008,10.02.2008] ";
		testTidaTimeMember(level,query,false);
	}
	@Test
	public void testPACombineOff2() {
			//use preaggregates
		String level = "";
		String query = "SELECT TIMESERIES OF count(TASKTYPE) ON TIME.RASTER.DAY "
				+ "FROM datasetTida IN [01.01.2008,10.02.2008] ";
		testTidaTimeMember(level,query,false);
	}

	public void testTidaTimeMember(final String level, final String query, final boolean usePA) {
	
			//create mockup data
		if(usePA) init.createMockupTidaTM(model, level);
	
		IQuery parsedQuery = queryFactory.parseQuery(query);
		IQueryResult resQuery = null;
		long start, end, result;
		result = start = end =  0;

		//fire x times the select query
	for(int i = 0; i < test_times ; i++) {
		start = System.currentTimeMillis();
		resQuery = queryFactory.evaluateQuery(parsedQuery,
				sessionManager);
		end = System.currentTimeMillis();
		result += end - start;
	}
	if(usePA) LOG.debug("[preaggregates used]");
	else LOG.debug("[preaggregates not used]");
		//output query time
	LOG.debug("queryTime average: " + result/test_times + " ms");
		//output query result
	if(resQuery != null) LOG.debug("resQuery: "+resQuery.toString());
	else LOG.debug("resQuery = null");		
		
		
	}
	
}
