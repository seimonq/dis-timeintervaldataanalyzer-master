package net.meisen.dissertation.model.parser.query;

import net.meisen.dissertation.jdbc.protocol.QueryType;
import net.meisen.dissertation.model.auth.IAuthManager;
import net.meisen.dissertation.model.auth.permissions.DefinedPermission;
import net.meisen.dissertation.model.data.TidaModel;
import net.meisen.dissertation.model.handler.TidaModelHandler;
import net.meisen.dissertation.server.CancellationException;
import net.meisen.general.genmisc.exceptions.ForwardedRuntimeException;

/**
 * A query instance used to retrieve data from the modeled {@code TidaSystem}.
 * 
 * @author pmeisen
 * 
 */
public interface IQuery {

	/**
	 * Specifies if the query needs a {@code TidaModel} for the model identified
	 * by the identifier retrieved via {@link #getModelId}. The method returns
	 * {@code true} if a {@code TidaModel} is needed, otherwise {@code false}.
	 * If {@code false} is set, the method
	 * {@link #evaluate(IAuthManager, TidaModelHandler, TidaModel, IResourceResolver)}
	 * will be called with {@code null} for the {@code TidaModel}. If
	 * {@code true} is returned by this method it is ensured that the
	 * {@code TidaModel} is set.
	 * 
	 * @return {@code true} if a model is needed, otherwise {@code false}
	 */
	public boolean expectsModel();

	/**
	 * Gets the identifier of the model the query should be evaluated for. The
	 * method can return {@code null} if no model is needed to evaluate the
	 * query.
	 * 
	 * @return the identifier of the model the query should be evaluated for, or
	 *         {@code null} if no model is needed
	 */
	public String getModelId();

	/**
	 * Sets the {@code modelId} for the query.
	 * 
	 * @param modelId
	 *            the {@code modelId} of the query
	 */
	public void setModelId(final String modelId);

	/**
	 * Evaluates the query against the specified {@code model}.
	 * 
	 * @param authManager
	 *            the {@code AuthManager} of the running application
	 * @param handler
	 *            the {@code TidaModelHandler} to retrieve or load additional
	 *            information
	 * @param model
	 *            the model used for evaluation
	 * @param resolver
	 *            a {@code ResourceResolver} which might be available (i.e. can
	 *            be {@code null}) to receive additional resources; if a
	 *            {@code ResourceResolver} is needed but not specified a
	 *            {@code ForwardedRuntimeException} should be thrown
	 * 
	 * @return the result of the query
	 * 
	 * @throws ForwardedRuntimeException
	 *             if an evaluation error occurs or if a needed {@code resolver}
	 *             is not specified
	 * @throws CancellationException
	 *             if the processing was cancelled by the client
	 */
	public IQueryResult evaluate(final IAuthManager authManager,
			final TidaModelHandler handler, final TidaModel model,
			final IResourceResolver resolver) throws ForwardedRuntimeException,
			CancellationException;

	/**
	 * Gets the type of the query.
	 * 
	 * @return the type of the query
	 * 
	 * @see QueryType
	 */
	public QueryType getQueryType();



	/**
	 * Enables the collection of identifiers for the query. For some queries it
	 * might be possible that the query collects identifiers of records which
	 * were modified, added or deleted. The concrete implementation of the
	 * evaluator of the query has to determine which identifiers are meaningful
	 * to be collected if activated. If the query does not support and
	 * collection or cannot collect anything the call can just be ignored.
	 * 
	 * @param enableIdCollection
	 *            {@code true} to enable the collection, otherwise {@code false}
	 */
	public void enableIdCollection(final boolean enableIdCollection);

	/**
	 * Method to retrieve sets of {@code DefinedPermissions}. Each set defines
	 * needed {@code DefinedPermissions} to process the query, i.e.
	 * 
	 * <pre>
	 * return new DefinedPermission[][] {
	 * 		new DefinedPermission[] { Permission.modify.create(modelId) },
	 * 		new DefinedPermission[] { Permission.modifyAll.create() } };
	 * </pre>
	 * 
	 * Defines two possibilities to process the query. The first one is given if
	 * the permission to modify the specific model (defined by it's identifier)
	 * is available. The second one is given if the permission to modify all
	 * instances is granted. It is also possible to define several permissions
	 * needed to process.
	 * 
	 * <pre>
	 * return new DefinedPermission[][] {
	 * 		new DefinedPermission[] { Permission.modify.create(modelId) },
	 * 		new DefinedPermission[] { Permission.modifyAll.create(),
	 * 				Permission.connectTSQL.create() } };
	 * </pre>
	 * 
	 * Defines again two possibilities to process the query. The first one is
	 * given if the permission to modify the specific model (defined by it's
	 * identifier) is available. The second one is given if the permission to
	 * modify all instances <b>and</b> the permission to connect via tsql is
	 * granted.
	 * 
	 * @return sets of needed permissions
	 */
	public DefinedPermission[][] getNeededPermissions();
}
