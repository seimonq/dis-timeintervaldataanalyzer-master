package net.kern.master.preaggregates;

import net.meisen.dissertation.impl.parser.query.DimensionSelector;
import net.meisen.general.genmisc.types.Objects;
import net.meisen.general.genmisc.types.Strings;

/**
 * selector used as identifier for preaggregates
 * 
 * consists of DimensionSelector and a MemberId 
 * 
 * @author skern
 *
 */
public class PreAggregateSelector extends DimensionSelector{

	private String memberId;
	/**
	 * creates complete PreAggregateSelector
	 *  
	 * @param dimensionId
	 * @param hierarchyId
	 * @param levelId
	 * @param memberId
	 */
	public PreAggregateSelector
		(String dimensionId, String hierarchyId, String levelId, String memberId) {
		
		super(dimensionId,hierarchyId,levelId);
		this.memberId = memberId;
	}
	/**
	 * creates comples PreAggreagteSelector with superClass
	 * @param dimSel
	 * @param memberId
	 */
	public PreAggregateSelector(DimensionSelector dimSel, String memberId) {
		super(dimSel.getDimensionId(),dimSel.getHierarchyId(),dimSel.getLevelId());
		this.memberId = memberId;
	}
	/**
	 * 
	 * @return the memberId
	 */
	public String getMemberId() {
		return memberId;
	}
	/**
	 * sets a new memberId
	 * @param memberId
	 */
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
	
	@Override
	public String toString() {
		return Strings.concate(".", getDimensionId(), getHierarchyId(),
				getLevelId(),getMemberId());
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (obj instanceof PreAggregateSelector) {
			final PreAggregateSelector ds = (PreAggregateSelector) obj;
			return Objects.equals(getDimensionId(), ds.getDimensionId())
					&& Objects.equals(getHierarchyId(), ds.getHierarchyId())
					&& Objects.equals(getLevelId(), ds.getLevelId())
					&& Objects.equals(getMemberId(), ds.getMemberId());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.generateHashCode(23, 7, getDimensionId(),
				getHierarchyId(), getLevelId(), getMemberId());
	}
	/**
	 * specialized compare function
	 * 
	 * @param ds
	 * 		the element compared with
	 * 
	 * @return 0 if equals and -1 else
	 */
	public int compareTo(final PreAggregateSelector ds) {
		int cmp = getDimensionId().compareTo(ds.getDimensionId());
		if (cmp == 0) {
			cmp = getHierarchyId().compareTo(ds.getHierarchyId());
			if (cmp == 0) {
				cmp = getLevelId().compareTo(getLevelId());
			}
				if (cmp == 0) {
					cmp = getMemberId().compareTo(getMemberId());
				}
		}
		return cmp;
	}
	/**
	 * 
	 * @return the superClass
	 */
	public DimensionSelector getDimensionSelector() {
		DimensionSelector dimSel = new DimensionSelector(getDimensionId(), 
				getHierarchyId(), getLevelId());
		
		return dimSel;
		
	}
}
