package net.meisen.dissertation.impl.parser.query.select.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.kern.master.preaggregates.PreAggregateException;
import net.kern.master.preaggregates.operations.SelectPreAggregates;
import net.meisen.dissertation.exceptions.QueryEvaluationException;
import net.meisen.dissertation.impl.parser.query.DimensionSelector;
import net.meisen.dissertation.impl.parser.query.select.DescriptorComperator;
import net.meisen.dissertation.impl.parser.query.select.DimensionComperator;
import net.meisen.dissertation.impl.parser.query.select.IComperator;
import net.meisen.dissertation.impl.parser.query.select.logical.DescriptorLeaf;
import net.meisen.dissertation.impl.parser.query.select.logical.DescriptorLogicTree;
import net.meisen.dissertation.impl.parser.query.select.logical.ILogicalTreeElement;
import net.meisen.dissertation.impl.parser.query.select.logical.LogicalOperator;
import net.meisen.dissertation.impl.parser.query.select.logical.LogicalOperatorNode;
import net.meisen.dissertation.model.data.DimensionModel;
import net.meisen.dissertation.model.data.DimensionModel.IMemberFilter;
import net.meisen.dissertation.model.data.MetaDataModel;
import net.meisen.dissertation.model.data.TidaModel;
import net.meisen.dissertation.model.descriptors.Descriptor;
import net.meisen.dissertation.model.descriptors.DescriptorModel;
import net.meisen.dissertation.model.dimensions.DescriptorMember;
import net.meisen.dissertation.model.indexes.BaseIndexFactory;
import net.meisen.dissertation.model.indexes.datarecord.TidaIndex;
import net.meisen.dissertation.model.indexes.datarecord.slices.Bitmap;
import net.meisen.dissertation.model.indexes.datarecord.slices.Slice;
import net.meisen.general.genmisc.exceptions.ForwardedRuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A evaluator to evaluate a {@code DescriptorLogicTree} based on a
 * {@code TidaModel}.
 * 
 * @author pmeisen
 * 
 */
public class DescriptorLogicEvaluator {
	private final static Logger LOG = LoggerFactory
			.getLogger(DescriptorLogicEvaluator.class);

	private final BaseIndexFactory indexFactory;
	private final TidaIndex index;
	private final MetaDataModel metaDataModel;
	private final DimensionModel dimensionModel;

	/**
	 * Constructor to create a evaluator used to evaluate a
	 * {@code DescriptorLogicTree}, used within the filter condition of a query.
	 * 
	 * @param model
	 *            the {@code TidaModel} to evaluate against
	 */
	public DescriptorLogicEvaluator(final TidaModel model) {

		// get the needed models
		dimensionModel = model.getDimensionModel();
		metaDataModel = model.getMetaDataModel();

		// get the index and the factory
		index = model.getIndex();
		indexFactory = model.getIndexFactory();
		
	}

	/**
	 * Calculates the bitmap representing the selected records of the specified
	 * filter, i.e. a single instance of the {@code Bitmap}. The created bitmap
	 * is an own instance, i.e. it is cloned or especially created using and/or
	 * operations.
	 * 
	 * @param tree
	 *            the filter to be applied
	 * 
	 * @return a bitmap representing the selected records
	 */
	public DescriptorLogicResult evaluateTree(final DescriptorLogicTree tree) {
		if (tree == null) {
			return null;
		}

		// get the root
		final LogicalOperatorNode root = tree.getRoot();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Evaluating logic-tree '" + tree + "'.");
		}

		// check if the root has children, otherwise there is nothing to do
		if (root.getChildren().size() != 0) {
			return new DescriptorLogicResult(evaluateNode(root));
		} else {
			return null;
		}
	}

	/**
	 * Evaluates a specific {@code node} of the tree.
	 * 
	 * @param node
	 *            the node to be evaluated
	 * 
	 * @return evaluates a specific {@code node} of the tree
	 */
	protected Bitmap evaluateNode(final LogicalOperatorNode node) {
		final List<Bitmap> members = new ArrayList<Bitmap>();

		final List<ILogicalTreeElement> children = node.getChildren();
		for (int i = children.size(); i > 0; i--) {
			final ILogicalTreeElement child = children.get(i - 1);

			if (child instanceof LogicalOperatorNode) {
				members.add(evaluateNode((LogicalOperatorNode) child));
			} else if (child instanceof DescriptorLeaf) {
				members.add(evaluateDescriptorLeaf((DescriptorLeaf) child));
			} else {
				throw new ForwardedRuntimeException(
						QueryEvaluationException.class, 1006, child);
			}
		}

		final Bitmap evalResult = evaluateOperator(node.get(), members);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Evaluated logic '" + node.get() + "' for members '"
					+ members + "' with result '" + evalResult + "'.");
		}

		return evalResult;
	}

	/**
	 * Evaluates the result of a {@code LogicalOperator}.
	 * 
	 * @param lo
	 *            the {@code LogicalOperator} to be evaluated
	 * @param intermediateBitmaps
	 *            the current results
	 * 
	 * @return the result of the application of the {@code LogicalOperator}
	 */
	protected Bitmap evaluateOperator(final LogicalOperator lo,
			final List<Bitmap> intermediateBitmaps) {

		if (LogicalOperator.NOT.equals(lo)) {
			if (intermediateBitmaps.size() != 1) {
				throw new ForwardedRuntimeException(
						QueryEvaluationException.class, 1003,
						intermediateBitmaps);
			}

			// apply the not
			return intermediateBitmaps.get(0).invert(index.getNextDataId() - 1);
		} else {
			final int size = intermediateBitmaps.size();

			if (size < 1) {
				throw new ForwardedRuntimeException(
						QueryEvaluationException.class, 1004,
						intermediateBitmaps);
			} else if (size > 1) {
				if (LogicalOperator.AND.equals(lo)) {
					return Bitmap.and(indexFactory,
							intermediateBitmaps.toArray());
				} else if (LogicalOperator.OR.equals(lo)) {
					return Bitmap.or(indexFactory,
							intermediateBitmaps.toArray());
				} else {
					throw new ForwardedRuntimeException(
							QueryEvaluationException.class, 1005, lo);
				}
			} else {
				return intermediateBitmaps.get(0);
			}
		}
	}

	/**
	 * Evaluates the result of the selection of a {@code Descriptor}.
	 * 
	 * @param leaf
	 *            the {@code DescriptorLeaf} to be evaluated
	 * 
	 * @return the result of the selection of the {@code Descriptor}
	 */
	protected Bitmap evaluateDescriptorLeaf(final DescriptorLeaf leaf) {
		final IComperator cmp = leaf.get();

		if (cmp instanceof DescriptorComperator) {
			return evaluateDescriptorComperator((DescriptorComperator) cmp);
		} else if (cmp instanceof DimensionComperator) {
			return evaluateDimensionComperator((DimensionComperator) cmp);
		} else {
			throw new ForwardedRuntimeException(QueryEvaluationException.class,
					1021, cmp == null ? null : cmp.getClass().getSimpleName());
		}
	}

	/**
	 * Evaluator used to evaluate a {@code DimensionComperator}. The
	 * implementation is currently really heavy, we should think about a better
	 * solution, i.e. by caching some information about the dimensions (e.g.
	 * each node should now it's leafs). [TODO increase performance]
	 * 
	 * @param cmp
	 *            the {@code DimensionComperator} to be evaluated
	 * 
	 * @return the result of the evaluation
	 */
	protected Bitmap evaluateDimensionComperator(final DimensionComperator cmp) {
		final DimensionSelector dimSelector = cmp.getDimension();

//******************************************************************************************************		
//extension for Kern's master theses
		
	//use preaggregates if there are any and return null if no preaggregates were
	//found for this dimension and hierarchy
		
		Bitmap bitmap = null;

		try {
			SelectPreAggregates selPAs = new SelectPreAggregates(dimensionModel.getPaModelHandler(), dimensionModel);
			//wildchars are not preaggregated
			if(!cmp.containsWildchar() && dimensionModel.isUsePreAggregates()) {
				bitmap = selPAs.useDescriptorMember(cmp);
			}
		}catch(PreAggregateException e) {
				if(LOG.isErrorEnabled()) LOG.error(e.getMessage());
		}
//******************************************************************************************************		

			//if preaggregates were not found or did not fit, get bitmap from descriptor index instead
		if(bitmap == null) {
			return dimensionModel.getBitmap(dimSelector, true, new IMemberFilter() {
	
				@Override
				public boolean accept(DescriptorMember member) {
					return cmp.matches(member.getId());
				}
	
				@Override
				public boolean selectOnlyOne() {
					return !cmp.containsWildchar();
				}
			});
		}
		return bitmap;

	}

	/**
	 * Evaluates a {@code DescriptorComperator}.
	 * 
	 * @param cmp
	 *            the {@code DescriptorComperator} to be evaluated
	 * 
	 * @return the result of the evaluation
	 */
	protected Bitmap evaluateDescriptorComperator(final DescriptorComperator cmp) {

		// get the addressed model
		final DescriptorModel<?> descModel = metaDataModel
				.getDescriptorModel(cmp.getId());
		if (descModel == null) {
			throw new ForwardedRuntimeException(QueryEvaluationException.class,
					1002, cmp.getId());
		}

		// get the addressed descriptor
		if (cmp.containsWildchar()) {

			/*
			 * In the case of wildchars, we have to combine all the descriptors
			 * hit by the selector, i.e. combine those all with an OR.
			 */
			final Collection<?> descriptors = descModel.getDescriptors();
			final List<Bitmap> bitmaps = new ArrayList<Bitmap>();
			for (final Object d : descriptors) {
				final Descriptor<?, ?, ?> desc = (Descriptor<?, ?, ?>) d;
				if (cmp.matches(desc.getUniqueString())) {
					final Slice<?> slice = index.getMetaIndexDimensionSlice(
							cmp.getId(), desc.getId());

					// add the slice if we have bitmap
					if (slice != null) {
						bitmaps.add(slice.getBitmap());
					}
				}
			}

			if (bitmaps.size() == 0) {
				return indexFactory.createBitmap();
			} else if (bitmaps.size() == 1) {
				return bitmaps.get(0);
			} else {
				return Bitmap.or(indexFactory, bitmaps.toArray());
			}
		} else {
			/*
			 * We just look for the one string and return the slice of the one
			 * descriptor
			 */
			final Descriptor<?, ?, ?> desc = descModel
					.getDescriptorByString(cmp.getValue());
			if (desc == null) {
				return indexFactory.createBitmap();
			} else {

				// use the index to retrieve the slice
				final Slice<?> slice = index.getMetaIndexDimensionSlice(
						cmp.getId(), desc.getId());

				// check if we have a slice, otherwise we assume null
				if (slice == null) {
					return indexFactory.createBitmap();
				} else {
					return slice.getBitmap();
				}
			}
		}
	}
}
