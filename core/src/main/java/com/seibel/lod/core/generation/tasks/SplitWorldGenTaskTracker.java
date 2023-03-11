package com.seibel.lod.core.generation.tasks;

import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import com.seibel.lod.core.generation.WorldGenerationQueue;

/**
 * Used to synchronize {@link WorldGenerationQueue} {@link WorldGenTask}'s
 * if the {@link WorldGenTask} needs to be split up.
 * 
 * @author Leetom
 * @version 2022-11-25
 */
public class SplitWorldGenTaskTracker implements IWorldGenTaskTracker
{
	public final IWorldGenTaskTracker parentTracker;
	public final CompletableFuture<Boolean> parentFuture;
	
	/** cached value to allow for quicker checking */
	public boolean isValid = true;
	
	
	
	public SplitWorldGenTaskTracker(IWorldGenTaskTracker parentTracker, CompletableFuture<Boolean> parentFuture)
	{
		this.parentTracker = parentTracker;
		this.parentFuture = parentFuture;
	}
	
	
	
	/** Recalculates and returns the new {@link SplitWorldGenTaskTracker#isValid} value */
	public boolean recalculateIsValid()
	{
		if (!this.isValid)
		{
			return false;
		}
		
		this.isValid = this.parentTracker.isMemoryAddressValid();
		if (!this.isValid)
		{
			this.parentFuture.complete(false);
		}
		
		return this.isValid;
	}
	
	@Override
	public boolean isMemoryAddressValid() { return this.isValid; }
	
	@Override
	public Consumer<ChunkSizedFullDataSource> getOnGenTaskCompleteConsumer() { return this.parentTracker.getOnGenTaskCompleteConsumer(); }
	
}
