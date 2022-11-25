package com.seibel.lod.core.generation.tasks;

import com.seibel.lod.core.datatype.full.ChunkSizedData;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Leetom
 * @version 2022-11-25
 */
public class SplitTaskTracker extends AbstractWorldGenTaskTracker
{
	public final AbstractWorldGenTaskTracker parentTracker;
	public final CompletableFuture<Boolean> parentFuture;
	public boolean cachedValid = true;
	
	
	
	public SplitTaskTracker(AbstractWorldGenTaskTracker parentTracker, CompletableFuture<Boolean> parentFuture)
	{
		this.parentTracker = parentTracker;
		this.parentFuture = parentFuture;
	}
	
	
	
	public boolean recheckState()
	{
		if (!this.cachedValid)
			return false;
		
		this.cachedValid = this.parentTracker.isValid();
		if (!this.cachedValid)
			this.parentFuture.complete(false);
		
		return this.cachedValid;
	}
	
	@Override
	public boolean isValid() { return this.cachedValid; }
	
	@Override
	public Consumer<ChunkSizedData> getConsumer() { return this.parentTracker.getConsumer(); }
	
}
