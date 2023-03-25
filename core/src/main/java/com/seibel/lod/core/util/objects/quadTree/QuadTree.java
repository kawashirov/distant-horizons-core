package com.seibel.lod.core.util.objects.quadTree;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.util.BitShiftUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.MathUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This class represents a quadTree of T type values.
 */
public class QuadTree<T>
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	
	/** The largest number detail level in this tree. */
    public final byte treeMaxDetailLevel;
	/** The smallest number detail level in this tree. */
    public final byte treeMinDetailLevel;
	
	/** contain the actual data in the quad tree structure */
    private final MovableGridRingList<QuadNode<T>> topRingList;
	
	private DhBlockPos2D centerBlockPos;
	
	private int widthInBlocks; 
	
	
	
	/**
     * Constructor of the quadTree
	 * @param widthInBlocks equivalent to the distance between two opposing sides 
     */
    public QuadTree(int widthInBlocks, DhBlockPos2D centerBlockPos, byte treeMinDetailLevel)
	{
        DetailDistanceUtil.updateSettings(); //TODO: Move this to somewhere else
		this.centerBlockPos = centerBlockPos;
		this.widthInBlocks = widthInBlocks;
		
		this.treeMinDetailLevel = treeMinDetailLevel;
		// the max detail level must be greater than 0 (to prevent divide by 0 errors) and greater than the minimum detail level
		this.treeMaxDetailLevel = (byte) Math.max(Math.max(1, this.treeMinDetailLevel), MathUtil.log2(widthInBlocks));
		
		int halfSizeInRootNodes = Math.floorDiv(this.widthInBlocks, 2) / BitShiftUtil.powerOfTwo(this.treeMaxDetailLevel);
		halfSizeInRootNodes = halfSizeInRootNodes + 1; // always add 1 so nodes will always have a parent, even if the tree's center is offset from the root node grid 
		
		Pos2D ringListCenterPos = new Pos2D(
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.x, this.treeMaxDetailLevel), 
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.z, this.treeMaxDetailLevel));
		this.topRingList = new MovableGridRingList<>(halfSizeInRootNodes, ringListCenterPos.x, ringListCenterPos.y);
        
    }
	
	
	
	//=====================//
	// getters and setters //
	//=====================//
	
    /** @return the value at the given section position */
    public final T get(DhSectionPos pos) throws IndexOutOfBoundsException { return this.getOrSet(pos, false, null); }
	/** @return the value that was previously in the given position, null if nothing */
	public final T set(DhSectionPos pos, T value) throws IndexOutOfBoundsException { return this.getOrSet(pos, true, value); }
	
	protected final T getOrSet(DhSectionPos pos, boolean setNewValue, T newValue) throws IndexOutOfBoundsException
	{
		if (this.isSectionPosInBounds(pos))
		{
			DhSectionPos rootPos = pos.convertToDetailLevel(this.treeMaxDetailLevel);
			int ringListPosX = rootPos.sectionX;
			int ringListPosZ = rootPos.sectionZ;
			
			QuadNode<T> topQuadNode = this.topRingList.get(ringListPosX, ringListPosZ);
			if (topQuadNode == null)
			{
				if (!setNewValue)
				{
					return null;
				}
				
				topQuadNode = new QuadNode<T>(rootPos, this.treeMinDetailLevel);
				boolean successfullyAdded = this.topRingList.set(ringListPosX, ringListPosZ, topQuadNode);
				LodUtil.assertTrue(successfullyAdded, "Failed to add top quadTree node at position: "+rootPos);
			}
			
			if (!topQuadNode.sectionPos.contains(pos))
			{
				LodUtil.assertNotReach("failed to get a root node that contains the input position: "+pos+" root node pos: "+topQuadNode.sectionPos);
			}
			
			
			T returnValue = topQuadNode.getValue(pos);
			if (setNewValue)
			{
				topQuadNode.setValue(pos, newValue);
			}
			return returnValue;
		}
		else
		{
			int radius = this.diameterInBlocks()/2;
			DhBlockPos2D minPos = this.getCenterBlockPos().add(new DhBlockPos2D(-radius, -radius));
			DhBlockPos2D maxPos =this.getCenterBlockPos().add(new DhBlockPos2D(radius, radius));
			throw new IndexOutOfBoundsException("QuadTree GetOrSet failed. Position out of bounds, min pos: "+minPos+", max pos: "+maxPos+", min detail level: "+this.treeMinDetailLevel+", max detail level: "+this.treeMaxDetailLevel+". Given Position: "+pos+" = block pos: "+pos.convertToDetailLevel(LodUtil.BLOCK_DETAIL_LEVEL));
		}
	}
	
	public boolean isSectionPosInBounds(DhSectionPos testPos)
	{
		// check if the testPos is within the detail level limits of the tree
		boolean detailLevelWithinBounds = this.treeMinDetailLevel <= testPos.sectionDetailLevel && testPos.sectionDetailLevel <= this.treeMaxDetailLevel;
		if (!detailLevelWithinBounds)
		{
			return false;
		}
		
		
		// check if the testPos is within the X,Z boundry of the tree
		DhBlockPos2D blockCornerOfTree = this.centerBlockPos.add(new DhBlockPos2D(-this.widthInBlocks/2,-this.widthInBlocks/2));
		DhLodPos cornerOfTreePos = new DhLodPos((byte)0, blockCornerOfTree.x, blockCornerOfTree.z);
		
		DhSectionPos sectionCornerOfInput = testPos.convertToDetailLevel((byte)0);
		DhLodPos cornerOfInputPos = new DhLodPos((byte)0, sectionCornerOfInput.sectionX, sectionCornerOfInput.sectionZ);
		int inputWidth = BitShiftUtil.powerOfTwo(testPos.sectionDetailLevel);
		
		return DoSquaresOverlap(cornerOfTreePos, this.widthInBlocks, cornerOfInputPos, inputWidth);
	}
	private static boolean DoSquaresOverlap(DhLodPos rect1Min, int rect1Width, DhLodPos rect2Min, int rect2Width)
	{
		// Determine the coordinates of the rectangles
		float rect1MinX = rect1Min.x;
		float rect1MaxX = rect1Min.x + rect1Width;
		float rect1MinZ = rect1Min.z;
		float rect1MaxZ = rect1Min.z + rect1Width;
		
		float rect2MinX = rect2Min.x;
		float rect2MaxX = rect2Min.x + rect2Width;
		float rect2MinZ = rect2Min.z;
		float rect2MaxZ = rect2Min.z + rect2Width;
		
		// Check if the rectangles overlap
		return rect1MinX < rect2MaxX && rect1MaxX > rect2MinX && rect1MinZ < rect2MaxZ && rect1MaxZ > rect2MinZ;
	}
	
	
	public int getNonNullChildCountAtPos(DhSectionPos pos) { return this.getChildCountAtPos(pos, false); }
	public int getChildCountAtPos(DhSectionPos pos, boolean includeNullValues)
	{
		int childCount = 0;
		for (int i = 0; i < 4; i++)
		{
			DhSectionPos childPos = pos.getChildByIndex(i);
			if (this.isSectionPosInBounds(childPos))
			{
				T value = this.get(childPos);
				if (includeNullValues || value != null)
				{
					childCount++;
				}
			}
		}
		
		return childCount;
	}
	
	
	/** no nulls TODO comment/rename */
	public void forEachRootNode(Consumer<QuadNode<T>> consumer)
	{
		this.topRingList.forEachOrdered((rootNode) -> 
		{
			if (rootNode != null)
			{
				consumer.accept(rootNode);
			}
		});
	}
	
	/** root nodes can be null */
	public void forEachRootNodePos(BiConsumer<QuadNode<T>, DhSectionPos> consumer)
	{
		this.topRingList.forEachPosOrdered((rootNode, pos2D) ->
		{
			DhSectionPos rootPos = new DhSectionPos(this.treeMaxDetailLevel, pos2D.x, pos2D.y);
			if (isSectionPosInBounds(rootPos))
			{
				consumer.accept(rootNode, rootPos);
			}
		});
	}
	
	public void forEachLeafValue(Consumer<? super T> consumer) { this.forEachLeafValue((value, sectionPos) -> { consumer.accept(value); }); }
	public void forEachLeafValue(BiConsumer<? super T, DhSectionPos> consumer)
	{
		this.forEachRootNode((rootNode) ->
		{
			rootNode.forAllLeafValues(consumer);
		});
	}
	
	
	
	
	//================//
	// get/set center //
	//================//
	
	public void setCenterBlockPos(DhBlockPos2D newCenterPos) { this.setCenterBlockPos(newCenterPos, null); }
	public void setCenterBlockPos(DhBlockPos2D newCenterPos, Consumer<? super T> removedItemConsumer)
	{
		this.centerBlockPos = newCenterPos;
		
		Pos2D expectedCenterPos = new Pos2D(
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.x, this.treeMaxDetailLevel), 
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.z, this.treeMaxDetailLevel));
		
		if (!this.topRingList.getCenter().equals(expectedCenterPos))
		{
			this.topRingList.moveTo(expectedCenterPos.x, expectedCenterPos.y, (quadNode) -> 
			{
				if (quadNode != null && removedItemConsumer != null)
				{
					removedItemConsumer.accept(quadNode.value);
				}
			});
		}
	}
	
	public final DhBlockPos2D getCenterBlockPos() { return this.centerBlockPos; }
	
	
	
	
	
	//==============//
	// base methods //
	//==============//
	
	public boolean isEmpty() { return this.leafNodeCount() == 0; } // TODO this should be rewritten to short-circuit
	
	public int leafNodeCount() 
	{
		AtomicInteger count = new AtomicInteger(0); 
		this.topRingList.forEachOrdered((node) -> 
		{
			node.forAllLeafValues((value) -> { count.addAndGet(1); });
		});
		
		return count.get();
	}
	
	// TODO comment, currently a tree will always have 9 root nodes, because the tree will grow all the way up to the top, if this is ever changed then these values must also change 
	public int ringListWidth() { return 3; }
	public int ringListHalfWidth() { return 1; }
	public int diameterInBlocks() { return this.widthInBlocks; }
	
//	public String getDebugString()
//	{
//		StringBuilder sb = new StringBuilder();
//		for (byte i = 0; i < this.ringLists.length; i++)
//		{
//			sb.append("Layer ").append(i + TREE_LOWEST_DETAIL_LEVEL).append(":\n");
//			sb.append(this.ringLists[i].toDetailString());
//			sb.append("\n");
//			sb.append("\n");
//		}
//		return sb.toString();
//	}
	
	@Override
	public String toString() { return "center block: "+this.centerBlockPos+", block width: "+this.widthInBlocks+", detail level range: ["+this.treeMinDetailLevel+"-"+this.treeMaxDetailLevel+"], leaf #: "+this.leafNodeCount(); }
	
}
