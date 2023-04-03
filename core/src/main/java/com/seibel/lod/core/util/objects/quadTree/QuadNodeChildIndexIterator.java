package com.seibel.lod.core.util.objects.quadTree;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Consumer;

public class QuadNodeChildIndexIterator<T> implements Iterator<Integer>
{
	private final Queue<Integer> iteratorQueue = new LinkedList<>();
	
	
	
	public QuadNodeChildIndexIterator(QuadNode<T> parentNode, boolean returnNullChildPos)
	{
		// only get the children if this section isn't at the bottom of the tree
		if (parentNode.sectionPos.sectionDetailLevel > parentNode.minimumDetailLevel)
		{
			// go over each child pos
			for (int i = 0; i < 4; i++)
			{
				// add index to queue if either not null or we want to return null values as well
				if (returnNullChildPos || parentNode.getChildByIndex(i) != null)
				{
					this.iteratorQueue.add(i);
				}
			}
		}
	}
	
	
	
	@Override
	public boolean hasNext() { return this.iteratorQueue.size() != 0; }
	
	@Override
	public Integer next()
	{
		if (!this.hasNext())
		{
			throw new NoSuchElementException();
		}
		
		
		return this.iteratorQueue.poll();
	}
	
	
	/** Unimplemented */
	@Override
	public void remove() { throw new UnsupportedOperationException("remove"); }
	
	@Override
	public void forEachRemaining(Consumer<? super Integer> action) { Iterator.super.forEachRemaining(action); }
	
}
