package com.seibel.lod.core.util.objects.quadTree;

import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.function.Consumer;

public class QuadNode<T>
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
	public final DhSectionPos sectionPos;
	public final byte minimumDetailLevel;
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
	
	
	
	public QuadNode(DhSectionPos sectionPos, byte minimumDetailLevel)
	{
		this.sectionPos = sectionPos;
		this.minimumDetailLevel = minimumDetailLevel;
	}
	
	
	
	/** @return the number of non-null child nodes */
	public int getChildCount() 
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
	
	/** returns the number of children that have non-null values */
	public int getChildValueCount()
	{
		int count = 0;
		for (int i = 0; i < 4; i++)
		{
			QuadNode<T> child = this.getChildByIndex(i);
			if (child != null && child.value != null)
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
	 * 0 = (0,0) - North West <br>
	 * 1 = (1,0) - South West <br>
	 * 2 = (0,1) - North East <br>
	 * 3 = (1,1) - South East <br>
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
				return swChild;
			case 2:
				return neChild;
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
		// debug validation
		
		if (!this.sectionPos.contains(inputSectionPos))
		{
			LOGGER.error((replaceValue ? "set " : "get ")+inputSectionPos+" center block: "+inputSectionPos.getCenter().getCornerBlockPos()+", this pos: "+this.sectionPos+" this center block: "+this.sectionPos.getCenter().getCornerBlockPos());
			throw new IllegalArgumentException("Input section pos "+inputSectionPos+" outside of this quadNode's pos: "+this.sectionPos+", this node's blockPos: "+this.sectionPos.convertToDetailLevel(LodUtil.BLOCK_DETAIL_LEVEL)+" block width: "+this.sectionPos.getWidth().toBlockWidth()+" input detail level: "+inputSectionPos.convertToDetailLevel(LodUtil.BLOCK_DETAIL_LEVEL)+" width: "+inputSectionPos.getWidth().toBlockWidth());
		}
		
		if (inputSectionPos.sectionDetailLevel > this.sectionPos.sectionDetailLevel)
		{
			throw new IllegalArgumentException("detail level higher than this node. Node Detail level: "+this.sectionPos.sectionDetailLevel+" input detail level: "+inputSectionPos.sectionDetailLevel);
		}
		
		if (inputSectionPos.sectionDetailLevel == this.sectionPos.sectionDetailLevel && !inputSectionPos.equals(this.sectionPos))
		{
			throw new IllegalArgumentException("Node and input detail level are equal, however positions are not; this tree doesn't contain the requested position. Node pos: "+this.sectionPos+", input pos: "+inputSectionPos);
		}
		
		if (inputSectionPos.sectionDetailLevel < this.minimumDetailLevel)
		{
			throw new IllegalArgumentException("Input position is requesting a detail level lower than what this node can provide. Node minimum detail level: "+this.minimumDetailLevel+", input pos: "+inputSectionPos);
		}
		
		
		
		// get/set logic
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
			
			DhSectionPos nwPos = this.sectionPos.getChildByIndex(0);
			DhSectionPos swPos = this.sectionPos.getChildByIndex(1);
			DhSectionPos nePos = this.sectionPos.getChildByIndex(2);
			DhSectionPos sePos = this.sectionPos.getChildByIndex(3);
			
			// look for the child that contains the input position (there may be a faster way to do this, but this works for now)
			QuadNode<T> childNode;
			if (nwPos.contains(inputSectionPos))
			{
				// TODO merge duplicate code
				if (replaceValue && this.nwChild == null)
				{
					// if no node exists for this position, but we want to insert a new value at this position, create a new node
					this.nwChild = new QuadNode<>(nwPos, this.minimumDetailLevel);
				}
				childNode = this.nwChild;
				
				// childNode should only be null when replaceValue = false and the end of a node chain has been reached
				return (childNode != null) ? childNode.getOrSetValue(inputSectionPos, replaceValue, newValue) : null;
			}
			else if (swPos.contains(inputSectionPos))
			{
				// TODO merge duplicate code
				if (replaceValue && this.swChild == null)
				{
					// if no node exists for this position, but we want to insert a new value at this position, create a new node
					this.swChild = new QuadNode<>(swPos, this.minimumDetailLevel);
				}
				childNode = this.swChild;
				
				// childNode should only be null when replaceValue = false and the end of a node chain has been reached
				return (childNode != null) ? childNode.getOrSetValue(inputSectionPos, replaceValue, newValue) : null;
			}
			else if (nePos.contains(inputSectionPos))
			{
				// TODO merge duplicate code
				if (replaceValue && this.neChild == null)
				{
					// if no node exists for this position, but we want to insert a new value at this position, create a new node
					this.neChild = new QuadNode<>(nePos, this.minimumDetailLevel);
				}
				childNode = this.neChild;
				
				// childNode should only be null when replaceValue = false and the end of a node chain has been reached
				return (childNode != null) ? childNode.getOrSetValue(inputSectionPos, replaceValue, newValue) : null;
			}
			else if (sePos.contains(inputSectionPos))
			{
				// TODO merge duplicate code
				if (replaceValue && this.seChild == null)
				{
					// if no node exists for this position, but we want to insert a new value at this position, create a new node
					this.seChild = new QuadNode<>(sePos, this.minimumDetailLevel);
				}
				childNode = this.seChild;
				
				// childNode should only be null when replaceValue = false and the end of a node chain has been reached
				return (childNode != null) ? childNode.getOrSetValue(inputSectionPos, replaceValue, newValue) : null;
			}
			else
			{
				throw new IllegalStateException("input position not contained by any node children. This should've been caught by the this.sectionPos.contains(inputPos) assert before this point.");
			}
		}
	}
	
	
	
	//===========//
	// iterators //
	//===========//
	
	public Iterator<QuadNode<T>> getNodeIterator() { return new QuadNodeIterator<>(this, false); }
	public Iterator<QuadNode<T>> getLeafNodeIterator() { return new QuadNodeIterator<>(this, true); }
	
	public Iterator<QuadNode<T>> getDirectChildNodeIterator() { return new QuadNodeDirectChildNodeIterator<>(this); }
	public Iterator<DhSectionPos> getDirectChildPosIterator() { return new QuadNodeDirectChildPosIterator<>(this); }
	
	
	
	//==========//
	// deletion //
	//==========//
	
	public void deleteAllChildren() { this.deleteAllChildren(null); }
	/** @param removedItemConsumer is only fired for non-null nodes, however the value passed in may be null */
	public void deleteAllChildren(Consumer<? super T> removedItemConsumer)
	{
		for (int i = 0; i < 4; i++)
		{
			QuadNode<T> childNode = this.getChildByIndex(i);
			if (childNode != null)
			{
				childNode.deleteAllChildren(removedItemConsumer);
			}
		}
		
		
		if (nwChild != null)
		{
			removedItemConsumer.accept(nwChild.value);
		}
		nwChild = null;
		
		if (neChild != null)
		{
			removedItemConsumer.accept(neChild.value);
		}
		neChild = null;
		
		if (seChild != null)
		{
			removedItemConsumer.accept(seChild.value);
		}
		seChild = null;
		
		if (swChild != null)
		{
			removedItemConsumer.accept(swChild.value);
		}
		swChild = null;
	}
	
	
	
	//==============//
	// base methods //
	//==============//
	
	@Override
	public String toString() { return "pos: "+this.sectionPos+", children #: "+this.getChildCount()+", value: "+this.value; }
	
}
