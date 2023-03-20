package com.seibel.lod.core.util.objects.quadTree;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class QuadNode<T>
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	public DhSectionPos sectionPos;
	public T value;
	
	
	/** 
	 * North West <br>
	 * index 0 <br>
	 * relative pos (0,0) 
	 */
	public QuadNode<T> nwChild;
	/**
	 * North East <br>
	 * index 1 <br>
	 * relative (1,0) 
	 */
	public QuadNode<T> neChild;
	/**
	 * South West <br>
	 * index 2 <br>
	 * relative (0,1) 
	 */
	public QuadNode<T> swChild;
	/**
	 * South East <br>
	 * index 3 <br>
	 * relative (1,1) 
	 */
	public QuadNode<T> seChild;
	
	
	
	
	
	public QuadNode(DhSectionPos sectionPos)
	{
		this.sectionPos = sectionPos;
	}
	
	
	/** @return the number of non-null child nodes */
	public int childCount() 
	{
		int count = 0;
		for (int i = 0; i < 4; i++)
		{
			if (this.getChildByIndex(i) != null)
			{
				count++;
			}
		}
		return count;
	}
	
	
	
	/**
	 * Returns the DhLodPos 1 detail level lower <br><br>
	 *
	 * Relative child positions returned for each index: <br>
	 * 0 = (0,0) <br>
	 * 1 = (1,0) <br>
	 * 2 = (0,1) <br>
	 * 3 = (1,1) <br>
	 *
	 * @param child0to3 must be an int between 0 and 3
	 */
	public QuadNode<T> getChildByIndex(int child0to3) throws IllegalArgumentException
	{
		switch (child0to3)
		{
			case 0:
				return nwChild;
			case 1:
				return neChild;
			case 2:
				return swChild;
			case 3:
				return seChild;
			
			default:
				throw new IllegalArgumentException("child0to3 must be between 0 and 3");
		}
	}
	
	
	/**
	 * @param sectionPos must be 1 detail level lower than this node's detail level
	 * @throws IllegalArgumentException if childSectionPos has the wrong detail level or is outside the bounds of this node
	 * @return the node at the given position
	 */
	public T getValue(DhSectionPos sectionPos) throws IllegalArgumentException { return this.getOrSetValue(sectionPos, false, null); }
	/**
	 * @param sectionPos must be 1 detail level lower than this node's detail level
	 * @throws IllegalArgumentException if childSectionPos has the wrong detail level or is outside the bounds of this node
	 * @return the node at the given position before the new node was set
	 */
	public T setValue(DhSectionPos sectionPos, T newValue) throws IllegalArgumentException { return this.getOrSetValue(sectionPos, true, newValue); }
	/**
	 * @param inputSectionPos must be 1 detail level lower than this node's detail level
	 * @throws IllegalArgumentException if childSectionPos has the wrong detail level or is outside the bounds of this 
	 * @return the node at the given position before the new node was set (if the new node should be set)
	 */
	private T getOrSetValue(DhSectionPos inputSectionPos, boolean replaceValue, T newValue) throws IllegalArgumentException
	{
		if (!this.sectionPos.contains(inputSectionPos))
		{
			LOGGER.error((replaceValue ? "set " : "get ")+inputSectionPos+" center block: "+inputSectionPos.getCenter().getCornerBlockPos()+", this pos: "+this.sectionPos+" this center block: "+this.sectionPos.getCenter().getCornerBlockPos());
			throw new IllegalArgumentException("Input section pos outside of this quadNode's range: "+this.sectionPos+" width: "+this.sectionPos.getWidth()+" input detail level: "+inputSectionPos+" width: "+inputSectionPos.getWidth());
		}
		
		if (inputSectionPos.sectionDetailLevel > this.sectionPos.sectionDetailLevel)
		{
			throw new IllegalArgumentException("detail level higher than this node. Node Detail level: "+this.sectionPos.sectionDetailLevel+" input detail level: "+inputSectionPos.sectionDetailLevel);
		}
		
		if (inputSectionPos.sectionDetailLevel == this.sectionPos.sectionDetailLevel && !inputSectionPos.equals(this.sectionPos))
		{
			throw new IllegalArgumentException("Node and input detail level are equal, however positions are not; this tree doesn't contain the requested position. Node pos: "+this.sectionPos+", input pos: "+inputSectionPos);
		}
		
		if (inputSectionPos.sectionDetailLevel == this.sectionPos.sectionDetailLevel)
		{
			// this node is the requested position
			T returnValue = this.value;
			if (replaceValue)
			{
				this.value = newValue;
			}
			return returnValue;
		}
		else
		{
			// this node is a parent to the position requested,
			// recurse to the next node
			
//			LOGGER.info((replaceValue ? "set " : "get ")+inputSectionPos+" center block: "+inputSectionPos.getCenter().getCornerBlockPos()+", this pos: "+this.sectionPos+" this center block: "+this.sectionPos.getCenter().getCornerBlockPos());
			
			DhLodPos nodeCenterPos = this.sectionPos.getCenter(); //.convertToDetailLevel((byte)0).getCenter();
			DhLodPos inputCenterPos = inputSectionPos.getCenter(); //.convertToDetailLevel((byte)0).getCenter();
			
			// may or may not be at the requested detail level
			QuadNode<T> childNode;
			if (inputCenterPos.x <= nodeCenterPos.x)
			{
				if (inputCenterPos.z <= nodeCenterPos.z)
				{
					// TODO merge duplicate code
					if (replaceValue && this.nwChild == null)
					{
						// if no node exists for this position, but we want to insert a new value at this position, create a new node
						this.nwChild = new QuadNode<>(this.sectionPos.getChildByIndex(0));
					}
//					LOGGER.info("NW");
					childNode = this.nwChild;
				}
				else
				{
					if (replaceValue && this.neChild == null)
					{
						this.neChild = new QuadNode<>(this.sectionPos.getChildByIndex(2));
					}
//					LOGGER.info("NE");
					childNode = this.neChild;
				}
			}
			else
			{
				if (inputCenterPos.z <= nodeCenterPos.z)
				{
					if (replaceValue && this.swChild == null)
					{
						this.swChild = new QuadNode<>(this.sectionPos.getChildByIndex(1));
					}
//					LOGGER.info("SW");
					childNode = this.swChild;
				}
				else
				{
					if (replaceValue && this.seChild == null)
					{
						this.seChild = new QuadNode<>(this.sectionPos.getChildByIndex(3));
					}
//					LOGGER.info("SE");
					childNode = this.seChild;
				}
			}
			
			
			if (childNode == null)
			{
				// should only happen when replaceValue = false and the end of a node chain has been reached
				return null;
			}
			else
			{
				return childNode.getOrSetValue(inputSectionPos, replaceValue, newValue);	
			}
		}
	}
	
	
	
	/** 
	 * Applies the given consumer to all 4 of this nodes' children. <br> 
	 * Note: this will pass in null children.
	 */
	public void forEachDirectChild(Consumer<QuadNode<T>> callback)
	{
		for (int i = 0; i < 4; i++)
		{
			callback.accept(this.getChildByIndex(i));
		}
	}
	
	/**
	 * Applies the given consumer to all leaf nodes below this node. <br> 
	 * Note: this will pass in null values.
	 */
	public void forAllLeafValues(Consumer<? super T> callback)
	{
		if (this.childCount() == 0)
		{
			// base case, bottom leaf node found
			callback.accept(this.value);
		}
		else
		{
			for (int i = 0; i < 4; i++)
			{
				QuadNode<T> childNode = this.getChildByIndex(i);
				if (childNode != null)
				{
					// TODO should this pass in a null value if the child node is null?
					childNode.forAllLeafValues(callback);
				}
			}
		}
	}
	
	
	@Override
	public String toString() { return "pos: "+this.sectionPos+", value: "+this.value; }
	
}
