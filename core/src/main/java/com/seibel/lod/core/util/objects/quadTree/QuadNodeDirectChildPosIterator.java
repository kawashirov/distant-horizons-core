package com.seibel.lod.core.util.objects.quadTree;

import com.seibel.lod.core.pos.DhSectionPos;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Consumer;

public class QuadNodeDirectChildPosIterator<T> implements Iterator<DhSectionPos>
{
	private final Queue<DhSectionPos> iteratorPosQueue = new LinkedList<>();
	
	
	
	public QuadNodeDirectChildPosIterator(QuadNode<T> parentNode, boolean returnNullChildPos) 
	{
		// go over each child pos
		for (int i = 0; i < 4; i++)
		{
			// add pos to queue if either not null or we want to return null values as well
			if (returnNullChildPos || parentNode.getChildByIndex(i) != null)
			{
				this.iteratorPosQueue.add(parentNode.sectionPos.getChildByIndex(i));
			}
		}
	}
	
	
	
	@Override
	public boolean hasNext() { return this.iteratorPosQueue.size() != 0; }
	
	@Override
	public DhSectionPos next()
	{
		if (!this.hasNext())
		{
			throw new NoSuchElementException();
		}
		
		
		DhSectionPos iteratorPos = this.iteratorPosQueue.poll();
		return iteratorPos;
	}
	
	
	/** Unimplemented */
	@Override
	public void remove() { throw new UnsupportedOperationException("remove"); }
	
	@Override
	public void forEachRemaining(Consumer<? super DhSectionPos> action) { Iterator.super.forEachRemaining(action); }
	
}
