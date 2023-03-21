package com.seibel.lod.core.util.objects.quadTree;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.util.BitShiftUtil;
import com.seibel.lod.core.util.DetailDistanceUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * This class represents a quadTree of T type values.
 */
public class QuadTree<T>
{
    public static final byte TREE_LOWEST_DETAIL_LEVEL = 0;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	
	/** The largest number detail level in this tree. */
    public final byte treeMaxDetailLevel;
	
	/** contain the actual data in the quad tree structure */
    private final MovableGridRingList<QuadNode<T>> topRingList;
	
	DhBlockPos2D centerBlockPos;
	
	
	
	/**
     * Constructor of the quadTree
	 * @param widthInBlocks equivalent to the distance between two opposing sides 
     */
    public QuadTree(int widthInBlocks, DhBlockPos2D centerBlockPos)
	{
        DetailDistanceUtil.updateSettings(); //TODO: Move this to somewhere else
		this.centerBlockPos = centerBlockPos;
		
		this.treeMaxDetailLevel = 10; // TODO we may need to make this dynamic // detail 10 = (2^10) 1024 blocks wide
		
//		int halfSize = 12; // TODO use this.treeMaxDetailLevel to determine
		int halfSize = Math.floorDiv(widthInBlocks, 2) / BitShiftUtil.powerOfTwo(this.treeMaxDetailLevel);
		halfSize = Math.max(halfSize, 1); // at minimum the ring list should have 3x3 (9) root nodes in it, to account for moving around 
		
		Pos2D ringListCenterPos = new Pos2D(
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.x, this.treeMaxDetailLevel), 
				BitShiftUtil.divideByPowerOfTwo(this.centerBlockPos.z, this.treeMaxDetailLevel));
		
		this.topRingList = new MovableGridRingList<>(halfSize, ringListCenterPos.x, ringListCenterPos.y);
        
    }// constructor
	
	
	
	//=====================//
	// getters and setters //
	//=====================//
	
    /** @return the value at the given section position */
    public final T get(DhSectionPos pos) throws IndexOutOfBoundsException { return this.getOrSet(pos, false, null); }
	/** @return the value that was previously in the given position, null if nothing */
	public final T set(DhSectionPos pos, T value) throws IndexOutOfBoundsException { return this.getOrSet(pos, true, value); }
	
	protected final T getOrSet(DhSectionPos pos, boolean setNewValue, T newValue) throws IndexOutOfBoundsException
	{
		if (this.isPositionInBounds(pos))
		{
			DhSectionPos rootPos = pos.convertToDetailLevel(this.treeMaxDetailLevel);
			int ringListPosX = rootPos.sectionX;
			int ringListPosZ = rootPos.sectionZ;
			
			QuadNode<T> topQuadNode = this.topRingList.get(ringListPosX, ringListPosZ);
			if (topQuadNode == null)
			{
				topQuadNode = new QuadNode<T>(rootPos);
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
			// TODO give the min and max allowed positions
			int width = this.widthInBlocks()/2;
			
			DhBlockPos2D minPos = this.getCenterBlockPos().add(new DhBlockPos2D(-width, -width));
			DhBlockPos2D maxPos =this.getCenterBlockPos().add(new DhBlockPos2D(width, width));
			throw new IndexOutOfBoundsException("QuadTree GetOrSet failed. Position out of bounds, min pos: "+minPos+", max pos: "+maxPos+", given Position: "+pos);
		}
	}
	
	
	private boolean isPositionInBounds(DhSectionPos pos)
	{
		DhSectionPos blockPos = pos.convertToDetailLevel(LodUtil.BLOCK_DETAIL_LEVEL);
		
		int halfWidthInBlocks = BitShiftUtil.powerOfTwo(this.treeMaxDetailLevel) * Math.floorDiv(this.topRingList.getWidth(), 2);
		
		int minX = this.centerBlockPos.x - halfWidthInBlocks;
		int maxX = this.centerBlockPos.x + halfWidthInBlocks;
		
		int minZ = this.centerBlockPos.z - halfWidthInBlocks;
		int maxZ = this.centerBlockPos.z + halfWidthInBlocks;
		
		return minX <= blockPos.sectionX && blockPos.sectionX <= maxX &&
				minZ <= blockPos.sectionZ && blockPos.sectionZ <= maxZ;
	}
	
	
	/** no nulls TODO */
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
	
	public void forEachLeafValue(Consumer<? super T> consumer)
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
		this.topRingList.forEachPos((node, pos) -> 
		{
			if (node != null)
			{
				node.forAllLeafValues((value) -> { count.addAndGet(1); });
			}
		});
		
		return count.get();
	}
	
	public int ringListWidth() { return this.topRingList.getWidth(); }
	public int widthInBlocks() { return this.ringListWidth() * BitShiftUtil.powerOfTwo(this.treeMaxDetailLevel); }
	
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
	
}
