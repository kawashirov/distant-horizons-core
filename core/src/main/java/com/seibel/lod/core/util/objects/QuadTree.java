package com.seibel.lod.core.util.objects;

import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.util.BitShiftUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * This class represents a quadTree of T type values.
 */
public class QuadTree<T>
{
    /**
     * Note: all config values should be via the class that extends this class, and
     *          by implementing different abstract methods
     */
    public static final byte TREE_LOWEST_DETAIL_LEVEL = ColumnRenderSource.SECTION_SIZE_OFFSET;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	public final byte getLayerDetailLevelOffset() { return ColumnRenderSource.SECTION_SIZE_OFFSET; }
	public final byte getLayerDetailLevel(byte sectionDetailLevel) { return (byte) (sectionDetailLevel - this.getLayerDetailLevelOffset()); }
	
    public final byte getLayerSectionDetailOffset() { return ColumnRenderSource.SECTION_SIZE_OFFSET; }
    public final byte getLayerSectionDetail(byte dataDetail) { return (byte) (dataDetail + this.getLayerSectionDetailOffset()); }
	
	
	/** AKA how many detail levels are in this quad tree */
    public final byte numbersOfSectionDetailLevels;
	/** related to {@link QuadTree#numbersOfSectionDetailLevels}, the largest number detail level in this tree. */
    public final byte treeMaxDetailLevel;
	
	/** contain the actual data in the quad tree structure */
    private final MovableGridRingList<T>[] ringLists;
	
    public final int blockRenderDistance;
	
	DhBlockPos2D centerBlockPos;
	
	
	
	/**
     * Constructor of the quadTree
     * @param viewDistance View distance in blocks
     */
    public QuadTree(
			int viewDistance, 
			DhBlockPos2D centerBlockPos)
	{
        DetailDistanceUtil.updateSettings(); //TODO: Move this to somewhere else
        this.blockRenderDistance = viewDistance;
		this.centerBlockPos = centerBlockPos;
		
		
        // Calculate the max section detail level //
		
		byte maxDetailLevel = this.getMaxDetailLevelInRange(viewDistance * Math.sqrt(2));
		this.treeMaxDetailLevel = this.getLayerSectionDetail(maxDetailLevel);
		this.numbersOfSectionDetailLevels = (byte) (this.treeMaxDetailLevel + 1);
		this.ringLists = new MovableGridRingList[this.numbersOfSectionDetailLevels - TREE_LOWEST_DETAIL_LEVEL];
        
		
		
		// Construct the ringLists //
		
		LOGGER.info("Creating "+MovableGridRingList.class.getSimpleName()+" with player center at "+this.centerBlockPos);
		for (byte sectionDetailLevel = TREE_LOWEST_DETAIL_LEVEL; sectionDetailLevel < this.numbersOfSectionDetailLevels; sectionDetailLevel++)
		{
			byte targetDetailLevel = this.getLayerDetailLevel(sectionDetailLevel);
			int maxDist = this.getFurthestBlockDistanceForDetailLevel(targetDetailLevel);
			
			// TODO temp fix that may or may not allocate the right amount, but it works well enough for now
//			int halfSize = MathUtil.ceilDiv(maxDist, BitShiftUtil.powerOfTwo(sectionDetailLevel)) + 8; // +8 to make sure the section is fully contained in the ringList //TODO what does the "8" represent?
			int halfSize = BitShiftUtil.powerOfTwo(this.treeMaxDetailLevel-targetDetailLevel); //MathUtil.ceilDiv(maxDist, sectionDetailLevel) + 8; // +8 to make sure the section is fully contained in the ringList //TODO what does the "8" represent?
			
			// check that the detail level and position are valid
			DhSectionPos checkedPos = new DhSectionPos(sectionDetailLevel, halfSize, halfSize);
			byte checkedDetailLevel = this.calculateExpectedDetailLevel(this.centerBlockPos, checkedPos);
			// validate the detail level
			LodUtil.assertTrue(checkedDetailLevel > targetDetailLevel,
					"in "+sectionDetailLevel+", getFurthestDistance would return "+maxDist+" which would be contained in range "+(halfSize-2)+", but calculateExpectedDetailLevel at "+checkedPos+" is "+checkedDetailLevel+" <= "+targetDetailLevel);
			
			
			// create the new ring list
			Pos2D ringListCenterPos = new Pos2D(BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.x, sectionDetailLevel), BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.z, sectionDetailLevel));
			
			LOGGER.info("Creating "+MovableGridRingList.class.getSimpleName()+" centered on "+ringListCenterPos+" with halfSize ["+halfSize+"] (maxDist ["+maxDist+"], dataDetail ["+targetDetailLevel+"])");
			this.ringLists[sectionDetailLevel - TREE_LOWEST_DETAIL_LEVEL] = new MovableGridRingList<>(halfSize, ringListCenterPos.x, ringListCenterPos.y);
			
		}
		
    }// constructor
	
	
	
	//=====================//
	// getters and setters //
	//=====================//
	
    /** @return the value at the given section position */
    public final T get(DhSectionPos pos) { return this.get(pos.sectionDetailLevel, pos.sectionX, pos.sectionZ); }
	/**
	 * @param detailLevel detail level of the section
	 * @param x x coordinate of the section
	 * @param z z coordinate of the section
	 * @return the value for the given section position
	 */
	public final T get(byte detailLevel, int x, int z) { return this.ringLists[detailLevel - TREE_LOWEST_DETAIL_LEVEL].get(x, z); }
	
	
	/** @return the value that was previously in the given position, null if nothing */
	public final T set(DhSectionPos pos, T value) { return this.set(pos.sectionDetailLevel, pos.sectionX, pos.sectionZ, value); }
	/** @return the value that was previously in the given position, null if nothing */
	public final T set(byte detailLevel, int x, int z, T value)
	{
		T previousValue = this.get(detailLevel, x, z);
		this.ringLists[detailLevel - TREE_LOWEST_DETAIL_LEVEL].set(x, z, value);
		return previousValue;
	}
	 
	
	
	//===============//
	// raw ringLists //
	//===============//
	
    /**
     * This method returns the RingList for the given detail level
     * @apiNote The returned ringList should not be modified! <br> TODO why? could it cause concurrent modification exceptions? is this only the case for {@link LodQuadTree}?
     * @param detailLevel the detail level
     * @return the RingList
     */
    public final MovableGridRingList<T> getRingList(byte detailLevel) { return this.ringLists[detailLevel - TREE_LOWEST_DETAIL_LEVEL]; }
	
	public Iterator<T> getRingListIterator(byte detailLevel) { return this.getRingList(detailLevel).iterator(); }
	
	
	
	//================//
	// get/set center //
	//================//
	
	public void setCenterPos(DhBlockPos2D newCenterPos) { this.setCenterPos(newCenterPos, null); }
	public void setCenterPos(DhBlockPos2D newCenterPos, Consumer<? super T> removedItemConsumer)
	{
		this.centerBlockPos = newCenterPos;
		
		// recenter the grid lists if necessary
		for (int sectionDetailLevel = TREE_LOWEST_DETAIL_LEVEL; sectionDetailLevel < this.numbersOfSectionDetailLevels; sectionDetailLevel++)
		{
			Pos2D expectedCenterPos = new Pos2D(BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.x, sectionDetailLevel), BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.z, sectionDetailLevel));
			MovableGridRingList<T> gridList = this.ringLists[sectionDetailLevel - TREE_LOWEST_DETAIL_LEVEL];
			
			if (!gridList.getCenter().equals(expectedCenterPos))
			{
				gridList.moveTo(expectedCenterPos.x, expectedCenterPos.y, removedItemConsumer);
			}
		}
	}
	
	public final DhBlockPos2D getCenterPos() { return this.centerBlockPos; }
	
	
	
	
	
	//===========================//
	// detail level calculations //
	//===========================//
    
    /**
     * This method will compute the detail level based on target position and section pos.
     * @param targetPos can be the player's position. A reference for calculating the detail level
     * @return detail level of this section pos
     */
    public final byte calculateExpectedDetailLevel(DhBlockPos2D targetPos, DhSectionPos sectionPos)
	{
        return DetailDistanceUtil.getDetailLevelFromDistance(
                targetPos.dist(sectionPos.getCenter().getCenterBlockPos()));
    }

    /**
     * Returns the highest detail level in a circle around the center.<br>
     * Note: the returned distance should always be the ceiling estimation of the circleRadius.
     * @return the highest detail level in the circle
     */
    public final byte getMaxDetailLevelInRange(double circleRadius) { return DetailDistanceUtil.getDetailLevelFromDistance(circleRadius); }

    /**
     * Returns the furthest distance to the center for the given detail level. <br>
     * Note: the returned distance should always be the ceiling estimation of the circleRadius.
     * @return the furthest distance to the center, in blocks
     */
    public final int getFurthestBlockDistanceForDetailLevel(byte detailLevl)
	{
        return (int)Math.ceil(DetailDistanceUtil.getDrawDistanceFromDetail(detailLevl + 1));
        // +1 because that's the border to the next detail level, and we want to include up to it.
    }
    
    /** Given a section pos at level n this method returns the parent value at level n+1 */
    public final T getParentValue(DhSectionPos pos) { return this.get(pos.getParentPos()); }
    
    /**
     * Given a section pos at level n and a child index, this returns the child section at level n-1
     * @param child0to3 since there are 4 possible children this index identifies which one we are getting
     */
    public final T getChildValue(DhSectionPos pos, int child0to3) { return this.get(pos.getChildByIndex(child0to3)); }
	
	
	
	//==============//
	// base methods //
	//==============//
	
	public boolean isEmpty()
	{
		for (byte detailLevel = QuadTree.TREE_LOWEST_DETAIL_LEVEL; detailLevel < this.treeMaxDetailLevel; detailLevel++)
		{
			if (!isDetailLevelEmpty(detailLevel))
			{
				return false;
			}
		}
		
		return true;
	}
	public boolean isDetailLevelEmpty(byte detailLevel) { return this.getRingList(detailLevel).isEmpty(); }
	
	/** returns the number of items in this QuadTree */
	public int size() 
	{
		int size = 0;
		for (byte detailLevel = QuadTree.TREE_LOWEST_DETAIL_LEVEL; detailLevel < this.treeMaxDetailLevel; detailLevel++)
		{
			size += getRingList(detailLevel).size();
		}
		
		return size;
	}
	
	
	public String getDebugString()
	{
		StringBuilder sb = new StringBuilder();
		for (byte i = 0; i < this.ringLists.length; i++)
		{
			sb.append("Layer ").append(i + TREE_LOWEST_DETAIL_LEVEL).append(":\n");
			sb.append(this.ringLists[i].toDetailString());
			sb.append("\n");
			sb.append("\n");
		}
		return sb.toString();
	}
	
}
