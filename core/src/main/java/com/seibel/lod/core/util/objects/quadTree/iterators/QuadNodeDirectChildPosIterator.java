package com.seibel.lod.core.util.objects.quadTree.iterators;

import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.objects.quadTree.QuadNode;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class QuadNodeDirectChildPosIterator<T> implements Iterator<DhSectionPos>
{
	private final QuadNodeChildIndexIterator<T> childIndexIterator;
	private final QuadNode<T> parentNode;
	
	
	public QuadNodeDirectChildPosIterator(QuadNode<T> parentNode)
	{
		this.parentNode = parentNode;
		this.childIndexIterator = new QuadNodeChildIndexIterator<>(this.parentNode, true);
	}
	
	
	
	@Override
	public boolean hasNext() { return this.childIndexIterator.hasNext(); }
	
	@Override
	public DhSectionPos next()
	{
		if (!this.hasNext())
		{
			throw new NoSuchElementException();
		}
		
		
		int childIndex = this.childIndexIterator.next();
		DhSectionPos sectionPos = this.parentNode.sectionPos.getChildByIndex(childIndex);
		return sectionPos;
	}
	
	
	/** Unimplemented */
	@Override
	public void remove() { throw new UnsupportedOperationException("remove"); }
	
	@Override
	public void forEachRemaining(Consumer<? super DhSectionPos> action) { Iterator.super.forEachRemaining(action); }
	
}